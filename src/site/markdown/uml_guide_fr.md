# Guide pour les spécifications UML

Les spécifications techniques et fonctionnelles sont écrites en UML : 
- Spécifications fonctionnelles : cas d'utilisation (objectif, acteurs, préconditions, scénario classique, scénarii alternatifs)
  et écrit en markdown 
- Spécifications techniques : 
  - Diagramme de classes: format PlantUML

## Spécifications fonctionnelles

Les specification fonctionnelles sont essentiellement des cas d'utilisation (Use Case).

Chaque cas d'utilisation est défini avec les sous parties suivantes :
- un diagramme `Use-Case`
- Objectifs
- Acteurs concernés
- Préconditions
- Scénario classique :
  - Numéroté chaque étape, afin de faciliter la lecture des scénarii alternatifs
- scénarii alternatifs :
  - Numéro à partir des numéros du scénario classique
- Post-conditions 
  
Les cas d'utilisation sont écrit en markdown, et les diagramme via PlantUML.

> Exemple: consultez les cas d'utilisation existants : https://gitlab.ifremer.fr/sih-public/sumaris/sumaris-doc/-/tree/master/use-case

## Spécifications techniques

### Diagrammes de classes

Voici les instructions pour la Génération de Diagrammes PlantUML :

1. **Classes**
   Utilisez le mot-clé `class` (et non pas `entity`).

2. **Omission des Classes Vides**:
   Ne déclarez pas explicitement les classes sans propriétés dans le code PlantUML. Elles seront affichées grâce aux relations définies.  
   Utilisez `hide empty members` pour ne pas afficher les membres vides et rendre le diagramme plus lisible.

3. **Relations et Cardinalités**:
   Représentez les relations entre les classes avec les cardinalités appropriées, en respectant la direction des relations.
   Indiquez clairement la direction des relations, surtout pour les relations unidirectionnelles.

4. Utilisez des couleurs ou déclarez des `package` pour isoler les classes provenant des référentiels ou de l'administration.