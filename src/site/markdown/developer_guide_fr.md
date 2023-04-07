# Guide du développeur

## Entités Java JPA

Voici les contraintes de génération d'une classe et de la modélisation UML :

- Convention de nommage dans les contraintes qui suivent:
  - "ID" représente le type de l'entité.
  - "E" réprésente la classe de l'entité.

- Classe Java `Entity` :
  - Utiliser Lombok @FieldNameConstants, @Getter et @Setter.
  - Indiquer également @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  - Impleménter `IEntity<ID>` en remplacant "ID" par le type de @id.
  - Utiliser @EqualsAndHashCode.Include sur la clef primaire.
  - Utiliser l'annotation @Table pour définir le nom de la table en utilisant des mots en minuscules et séparés par des underscores (snake_case).
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
  - Pour chaque entités liées, ajouter aussi un attribut portant uniquement l'identifiant. Par exemple : `private Integer entityId`, en remplaçant `entity` par le nom de l'attribut.
  - Pour les collections de type simple (String, Integer, etc), préférer des `array` aux `List<>`.
  - Pour les collections de classes VO, préférer l'usage de `List<>`.
  - Ignorer la génération des imports.

- Interface `Specifications` pour la spécification des requêtes via JPA
  - Étendre l'interface `extends IEntitySpecifications<ID, E>` en remplaçant "ID" par le type de la clef primaire, et `E` par la classe de l'entité.

- Interface `Repository` pour l'accès aux données via JPA
  - Étendre l'interface `SumarisJpaRepository<E extends IEntity<ID>, V extends IValueObject<ID>` en remplacant "ID" par le type de la clef primaire. 
  - Implémenter l'interface `Specifications` correspondante à l'entité (par exemple 'MaClasseSpecifications`).

- Classe `RepositoryImpl` :
  - Implémente l'interface `Specifications` correspondantes à l'entité. Il n'est pas utile d'implémenter l'interface `Repository`, qui sera injecté par JPA dans l'objet final.
  - Étendre la classe `SumarisJpaRepositoryImpl<E, ID, V>`, en remplaçant "E" est la classe de l'entité, "ID" le type de sa clef, "V" la classe ValueObject (VO)  
  - Déclarer un constructeur comme suit, en remplaçant "E" par la classe de l'entité : 
    ```java
    protected RepositoryImpl(EntityManager entityManager) {
      super(E.class, E.class, entityManager);
    }
    ```
- Déclarer les méthodes :
  ```java
  public void toVO(E source, V target, boolean copyIfNull);`
  public void toEntity(V source, E target, boolean copyIfNull);`
  ```
  - Dans la méthode `toVO()`, pour chaque clef étrangères de la classe entité, ajouter une conversion du type :
  ```java
      // Convertir chaque sous-entité en VO, en utilisant les Repository liés.
      // Déclarer le Repository supplémentaire grâce à l'annotation @Ressource. 
      // Par exemple, pour l'attribut `otherEntity` de type `OtherEntity` :
      if (copyIfNull || source.getOtherEntity() != null) {
          if (source.getOtherEntity() == null) {
              target.setOtherEntity(null);
          }
          else {
              OtherEntityVO otherEntityVO = otherEntityRepository.toVO(source.getOtherEntity());    
              target.setOtherEntity(otherEntityVO);
          }
      }
  ``` 
  - Dans la méthode `toEntity()`, pour chaque clefs étrangères de la classe entité, ajouter une conversion du type :    
    ```java
        // Convertir chaque sous-entité, en récupérant l'identifiant soit par la propriété `entityId`, soit par le VO
        // Par exemple, pour l'attribut `otherEntity` de type `OtherEntity` :
        Integer otherEntityId = source.getOtherEntityId() != null ? source.getOtherEntityId() : (source.getEntity() != null ? source.getOtherEntity().getId() : null);
        if (copyIfNull || (otherEntityId != null)) {
            if (otherEntityId == null) {
                target.setOtherEntity(null);
            }
            else {
                target.setOtherEntity(getReference(OtherEntity.class, otherEntityId));
            }
        }
    ``` 

- Modélisation PlantUML :
  - Classe courante en bleu ciel, entités liées en gris.


## Service Java et GraphQL

- Une classe `FilterVO` :
  - Préfixer la classe par le nom de l'entité.
  - Utiliser Lombok avec @Data, @Builder, @FieldNameConstants
  - Impleménter l'interface `IDataFilter` et `Serializable`.
  - Ajouter les attributs suivant, issus de IDataFilter : 
    ```java
    private Integer recorderDepartmentId;
    private Integer[] qualityFlagIds;
    private DataQualityStatusEnum[] dataQualityStatus;
    ```
  - Ignorer la génération des imports.

- Une classe `FetchOptions` :
  - Préfixer la classe par le nom de l'entité.
  - Utiliser Lombok avec @Data, @Builder
  - Impleménter l'interface `IFetchOptions`
  - Ignorer la génération des imports.

- Une interface `Service` est implémentée par chaque classe entity :
  - Préfixer l'interface par le nom de l'entité.
  - Utiliser l'annotation Spring @Transactional
  - Déclarer les méthodes suivantes, en remplaçant :
    - "F" par son `FilterVO"`.
    - "FO" par son `FetchOptions`.
    ```java
    List<E> findByFilter(F filter, Page page, FO fetchOptions);
    Optional<E> findById(ID id, FO fetchOptions);
    E save(E source);
    void delete(ID id);
    ```
  - Ignorer la génération des imports.

- Une classe `ServiceImpl`, implémente l'interface `Service` correspondante :
  - Préfixer la classe par le nom de l'entité.
  - Utiliser l'annotation Spring `@Service("entityService")` en remplaçant "entity" par le nom de l'entité (sans majuscule). 
  - Déclarer le `Repository` de l'entité correspondante via l'annotation @Resource
  - Déclarer chaque méthode de l'interface `Service`, en deleguant l'appel au `Repository` de l'entité.
  - Ignorer la génération des imports.

- Une classe `GraphQLService` :
  - Préfixer la classe par le nom de l'entité.
  - Utiliser Lombok avec@RequiredArgsConstructor
  - Ajouter les annotations @Service, @RequiredArgsConstructor @GraphQLApi et @ConditionalOnWebApplication
  - Déclarer le service de l'entité correspondante via l'annotation @Resource
  - Pour chaque méthode déclarée dans l'interface `Service`, déclarer une méthode identifique (même nom et paramètres) en y ajoutant :
    - Une annotation `@GraphQLQuery(name = "<methodName>", description = "<methodDescription>")` en remplacant "<methodName>" par le nom de la fonction, et et "<methodDescription>" par sa documentation.  
    - Une annotation `@GraphQLArgument(name = "<parameterName>")` pour chaque paramètre de la méthode, en remplaçant "<parameterName>" par son nom.
  - Ignorer la génération des imports.

## Gestion des mise à jour BDD

Voici les contraintes de génération d'un changelog Liquibase pour la mise à jour de la base de données :

- Utiliser l'email du développeur comme auteur. Par exemple "benoit.lavenier@e-is.pro".
- Inclure toujours une précondition vérifiant que la table est absente.
- Générer des changeset avec des id qui se suivent, sous la forme <timestamp_ms>-NNN.
- Utiliser "_fk" pour les clés étrangères.
- Utiliser une séquence pour l'identifiant, avec un nom suffixé par "_seq".
- Utiliser le suffixe "_fkc" pour les noms de contraintes de clés étrangères.
- Créer une version de changelog pour chaque base de données parmi : HsqlDB, PostgreSQL et Oracle. 
  - Les id des changeset doivent être identiques entre ces trois versions.

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
