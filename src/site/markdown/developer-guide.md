# Guide du développeur

## Entités JPA

Voici les contraintes de génération d'une classe et de la modélisation UML :

- Classe Java :
  - Utiliser Lombok @FieldNameConstants, @Getter et @Setter.
  - Indiquer également @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  - Impleménter `IEntity<ID>` en remplacant "ID" par le type de @id.
  - Utiliser @EqualsAndHashCode.Include sur la clef primaire (@id)
  - Utiliser "_fk" pour les clés étrangères.
  - Utiliser des séquences pour les @Id, avec un nom préfixé en "_seq".
  - Toutes les colonnes sont obligatoires (nullable = false).
  - Ignorer la génération des imports.


- Classe de transport (`ValueObject` ou `VO`) :
  - Utiliser Lombok avec @Data et @FieldNameConstants.
  - Impleménter l'interface `IValueObject<ID>`, en remplacant "ID" par le type de la clef primaire. 
  - Nommer la attributs comme pour la classe entité correspondante.
    - Ne pas oublier d'y mettre les attributs `id` et `updateDate`.
  - Classes des classes entités liées suffixés aussi en `VO`.
  - Pour chaque entités liées, ajouter aussi un attribut portant uniquement l'identifiant. Par exemple : `private Integer entityId`.
  - Pour les collections de type simple (String, Integer, etc), préférer des `array` aux `List<>`.
  - Pour les collections de classes VO, préférer l'usage de `List<>`.
  - Ignorer la génération des imports.

- Interface `Specifications` pour la spécification des requêtes via JPA
  - Étendre l'interface `extends IEntitySpecifications<ID, E>` en remplaçant "ID" par le type de la clef primaire, et `E` par la classe de l'entité.

- Interface `Repository` pour l'accès aux données via JPA
  - Étendre l'interface `SumarisJpaRepository<E extends IEntity<ID>, V extends IValueObject<ID>` en remplacant "ID" par le type de la clef primaire. 
  - Implémenter l'interface `Specifications` correspondante à l'entité (par exemple 'MaClasseSpecifications`).

- Classe `RepositoryImpl` :
  - Implémente les interfaces `Repository` et `Specifications` correspondantes à l'entité.
  - Étendre la classe `SumarisJpaRepositoryImpl<E, ID, V>`, en remplaçant "E" est la classe de l'entité, "ID" le type de sa clef, "V" la classe ValueObject (VO)  
  - Déclarer un constructeur comme suit, en remplaçant `MyEntity` par le nom de la classe d'entité : 
    ```java
    protected MyEntityRepositoryImpl(EntityManager entityManager) {
      super(MyEntity.class, MyEntityVO.class, entityManager);
    }
    ```
  - Déclarer les méthodes :
    ```java
    public void toVO(E source, V target, boolean copyIfNull);`
    public voic toEntity(V source, E target, boolean copyIfNull);`
    ```
  - Dans la méthode `toEntity()`, pour chaque clefs étrangères de la classe entité, ajouter une conversion du type :    
    ```java
        // Pour chaque sous-entité de la classe Entity
        Integer entityId = source.getEntityId() != null ? source.getEntityId() : (source.getEntity() != null ? source.getEntity().getId() : null);
        if (copyIfNull || (entityId != null)) {
            if (entityId == null) {
                target.setEntity(null);
            }
            else {
                target.setEntity(getReference(Entity.class, entityId));
            }
        }
    ``` 

- Modélisation PlantUML :
  - Classe courante en bleu ciel, entités liées en gris.

## Entités typescript

Voici les contraintes de génération d'une classe typescript :
- Classe typescript
  - Ajouter l'annotation @EntityClass({typename: 'MaClasseVO'}) 
    - En remplacant 'MaClasseVO' par la classe courant et en y ajoutant le suffixe "VO".
  - Ajouter des paramètres de classe `<T, ID, AO, FO>` avec :
    - `T` comme classe par défaut la classe elle même
    - `ID` comme type par défaut `number`
    - `AO` comme type par défaut `EntityAsObjectOptions`
    - `FO` comme type par défaut `any`
  - Étendre la classe Entity<T, ID>
    - Les attributs id et updateDate sont présents dans la classe parente `Entity` et n'ont pas besoin d'être ajoutés à la classe dérivée.
  - Ajouter une méthode statique : `static fromObject(source: any, options?: any) => MaClasse`
  - Ajouter un construteur
- Ajouter deux méthodes `asObject()` et `fromObject()` définie comme suit 
  ```ts
    asObject(opts?: AO): any {
      const target: any = super.asObject(opts);
      // Can be completed here, if need

      // Les dates doivent explicitement être converti en `string` en utilisant la méthode statique `toDateISOString`, par exemple :
      target.myDate = toDateISOString(this.myDate);
      // Les attributes qui sont des entités doivent être convertie également via leur fucntion `asObject()`, par exemple :
      target.myEntity = this.myEntity && this.myEntity.asObject(opts); 
      return target;
    }

    fromObject(source: any, opts?: FO) {  
        super.fromObject(source, opts);
        // Lister ici tous les attributs de la classe, recopiés depuis le paramètre `source`.
        this.myAttribute = source.myAttribute;
        // Les dates doivent utiliser la classe `Moment` et être recopié dans `fromObject()` en utilisant la méthode statique `fromDateISOString`, par exemple :
        this.myDate = fromDateISOString(source.<myDate>)
        // Les entités liées doivent être recopiée via leur fonction statique `Entity1.fromObject(entite1, opts)`, par exemple :
        this.myEntity = Entite1.fromObject(source.myEntity); 
    }
  ```
  - Ignorer la génération des imports.
