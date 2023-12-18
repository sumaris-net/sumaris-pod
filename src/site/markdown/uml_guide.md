# UML Specifications Guide

Technical and functional specifications are written in UML:
- Functional Specifications: 
  - UML use cases (objective, actors, preconditions, standard scenario, alternative scenarios)
    written using the markdown format. 
- Technical Specifications:
   - UML Class Diagram

## Functional Specifications

Functional specifications are primarily use cases (Use Case).

Each use case is defined with the following subsections:
- a `Use-Case` Diagram
- Objectives
- Involved Actors
- Preconditions
- Standard Scenario:
   - Number each step to facilitate the reading of alternative scenarios
- Alternative scenarios:
   - Number starting from the numbers of the standard scenario
- Post-conditions

Use cases are written in markdown, and diagrams are created using PlantUML.

> Example: See existing use case at: https://gitlab.ifremer.fr/sih-public/sumaris/sumaris-doc/-/tree/master/use-case

## Technical Specifications

### Class Diagrams

Here are the instructions for Generating PlantUML Diagrams:

1. **Classes**
   Use the keyword `class` (not `entity`).

2. **Omitting Empty Classes**:
   Do not explicitly declare classes without properties in PlantUML code. They will be displayed through the defined relationships.  
   Use `hide empty members` to not display empty members and make the diagram more readable.

3. **Relationships and Cardinalities**:
   Represent relationships between classes with appropriate cardinalities, respecting the direction of relationships.
   Clearly indicate the direction of relationships, especially for unidirectional relationships.

4. Use colors or declare `package` to isolate classes from repositories or administration.
