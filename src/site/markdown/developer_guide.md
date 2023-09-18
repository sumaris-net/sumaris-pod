# Core > Developer Guide

## Java JPA Entities

Here are the generation constraints for a class and UML modeling:

- Naming convention in the following constraints:
  - "ID" represents the entity type.
  - "E" represents the entity class.

- Java `Entity` class:
  - Use Lombok @FieldNameConstants, @Getter, and @Setter.
  - Also indicate @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  - Implement `IEntity<ID>` by replacing "ID" with the @id type.
  - Use @EqualsAndHashCode.Include on the primary key.
  - Use the @Table annotation to define the table name using lowercase words separated by underscores (snake_case).
  - Use "_fk" for foreign keys.
  - Use sequences for @Id, with a name prefixed with "_seq".
  - All columns are required (nullable = false).
  - Ignore import generation.


- Transport class (`ValueObject` or `VO`):
  - Use Lombok with @Data and @FieldNameConstants.
  - Implement the `IValueObject<ID>` interface, replacing "ID" with the primary key type.
  - Name the attributes as for the corresponding entity class.
    - Do not forget to include the `id` and `updateDate` attributes.
  - Classes of linked entity classes also suffixed with `VO`.
  - For each linked entity, also add an attribute containing only the identifier. For example: `private Integer entityId`, replacing `entity` with the attribute name.
  - For simple type collections (String, Integer, etc.), prefer `array` to `List<>`.
  - For VO class collections, prefer using `List<>`.
  - Ignore import generation.

- `Specifications` interface for JPA query specifications
  - Extend the interface `extends IEntitySpecifications<ID, E>` by replacing "ID" with the primary key type, and `E` with the entity class.

- `Repository` interface for data access via JPA
  - Extend the interface `SumarisJpaRepository<E extends IEntity<ID>, V extends IValueObject<ID>` by replacing "ID" with the primary key type.
  - Implement the `Specifications` interface corresponding to the entity (for example, 'MyClassSpecifications`).

- `RepositoryImpl` class:
  - Implement the `Specifications` interface corresponding to the entity. It is not necessary to implement the `Repository` interface, which will be injected by JPA into the final object.
  - Extend the class `SumarisJpaRepositoryImpl<E, ID, V>`, replacing "E" with the entity class, "ID" with its key type, and "V" with the ValueObject (VO) class.
  - Declare a constructor as follows, replacing "E" with the entity class:
    ```java
    protected RepositoryImpl(EntityManager entityManager) {
      super(E.class, E.class, entityManager);
    }
    ```
  - Declare the methods:
    ```java
    public void toVO(E source, V target, boolean copyIfNull);
    public void toEntity(V source, E target, boolean copyIfNull);
    ```
    - In the toVO() method, for each foreign key in the entity class, add a conversion like this:
    ```java
    // Convert each sub-entity to VO, using the related Repositories.
    // Declare the additional Repository using the @Resource annotation. 
    // For example, for the `otherEntity` attribute of type `OtherEntity`:
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
  - In the `toEntity()` method, for each foreign key in the VO class, add a conversion like this:
  ```java
      // Convert each sub-VO to an entity, using the related Repositories.
      // Declare the additional Repository using the @Resource annotation.
      // For example, for the `otherEntityId` attribute of type `Integer`:
      if (copyIfNull || source.getOtherEntityId() != null) {
          if (source.getOtherEntityId() == null) {
              target.setOtherEntity(null);
          } else {
              target.setOtherEntity(otherEntityRepository.getById(source.getOtherEntityId()));
          }
      }
  ```

- Model diagrams with PlantUML :
  - Current class in light blue, and associated entities in light gray.

## Java Service and GraphQL

- A `FilterVO` class:
  - Prefix the class with the name of the entity.
  - Use Lombok with @Data, @Builder, @FieldNameConstants
  - Implement the `IDataFilter` and `Serializable` interfaces.
  - Add the following attributes, from IDataFilter:
    ```java
    private Integer recorderDepartmentId;
    private Integer[] qualityFlagIds;
    private DataQualityStatusEnum[] dataQualityStatus;
    ```
  - Ignore the generation of imports.

- A `FetchOptions` class:
  - Prefix the class with the name of the entity.
  - Use Lombok with @Data, @Builder
  - Implement the `IFetchOptions` interface
  - Ignore the generation of imports.

- A `Service` interface is implemented by each entity class:
  - Prefix the interface with the name of the entity.
  - Use the Spring @Transactional annotation
  - Declare the following methods, replacing:
    - "F" with its `FilterVO`.
    - "FO" with its `FetchOptions`.
    ```java
    List<E> findByFilter(F filter, Page page, FO fetchOptions);
    Optional<E> findById(ID id, FO fetchOptions);
    E save(E source);
    void delete(ID id);
    ```
  - Ignore the generation of imports.

- A `ServiceImpl` class implements the corresponding `Service` interface:
  - Prefix the class with the name of the entity.
  - Use the Spring `@Service("entityService")` annotation, replacing "entity" with the name of the entity (lowercase).
  - Declare the corresponding entity `Repository` using the @Resource annotation
  - Declare each method of the `Service` interface, delegating the call to the entity's `Repository`.
  - Ignore the generation of imports.

- A `GraphQLService` class:
  - Prefix the class with the name of the entity.
  - Use Lombok with @RequiredArgsConstructor
  - Add the annotations @Service, @RequiredArgsConstructor, @GraphQLApi, and @ConditionalOnWebApplication
  - Declare the corresponding entity service using the @Resource annotation
  - For each method declared in the `Service` interface, declare an identical method (same name and parameters) by adding:
    - For a read function: 
      - A `@GraphQLQuery(name = "&lt;methodName>", description = "&lt;methodDescription>")` annotation, replacing "&lt;methodName>" with the function name, and "&lt;methodDescription>" with its documentation.
      - A `@Transactional(readOnly = true)` annotation.
    - For a write function (with a prefix 'save' or 'delete'): 
      - A `@GraphQLMutation(name = "&lt;methodName>", description = "&lt;methodDescription>")` annotation, replacing "&lt;methodName>" with the function name, and "&lt;methodDescription>" with its documentation.
      - A `@Transactional` annotation.
    - A `@GraphQLArgument(name = "&lt;parameterName>")` annotation for each method parameter, replacing "&lt;parameterName>" with its name.
  - Ignore the generation of imports.

## Database update management

Here are the constraints for generating a Liquibase changelog for updating the database:

- Use the developer's email as the author. For example, "benoit.lavenier@e-is.pro".
- Always include a precondition checking that the table is absent.
- Generate changesets with sequential ids in the form <timestamp_ms>-NNN.
- Use "_fk" for foreign keys.
- Use a sequence for the identifier, with a name suffixed by "_seq".
- Use the suffix "_fkc" for foreign key constraint names.
- Create a changelog version for each database among: HsqlDB, PostgreSQL, and Oracle.
  - Changeset ids must be identical across these three versions.

## TypeScript Entities

Here are the generation constraints for a TypeScript class:
- TypeScript Class
  - Add the annotation @EntityClass({typename: 'MyClassVO'})
    - Replace 'MyClassVO' with the current class and add the "VO" suffix.
  - Add class parameters `<T, ID, AO, FO>` with:
    - `T` as the default class being the class itself
    - `ID` as the default type `number`
    - `AO` as the default type `EntityAsObjectOptions`
    - `FO` as the default type `any`
  - Extend the class Entity&lt;T, ID>
    - The attributes id and updateDate are present in the parent class `Entity` and do not need to be added to the derived class.
  - Add a static method: `static fromObject(source: any, options?: any) => MyClass`
  - Add a constructor
- Add two methods `asObject()` and `fromObject()` defined as follows
  ```ts
    asObject(opts?: AO): any {
      const target: any = super.asObject(opts);
      // Can be completed here, if needed

      // Dates must explicitly be converted to `string` using the static method `toDateISOString`, for example:
      target.myDate = toDateISOString(this.myDate);
      // Attributes that are entities must also be converted via their `asObject()` function, for example:
      target.myEntity = this.myEntity && this.myEntity.asObject(opts);
      return target;
    }

    fromObject(source: any, opts?: FO) {
        super.fromObject(source, opts);
        // List all the class attributes here, copied from the `source` parameter.
        this.myAttribute = source.myAttribute;
        // Dates should use the `Moment` class and be copied in `fromObject()` using the static method `fromDateISOString`, for example:
        this.myDate = fromDateISOString(source.&lt;myDate>)
        // Related entities should be copied via their static function `Entity1.fromObject(entity1, opts)`, for example:
        this.myEntity = Entity1.fromObject(source.myEntity);
    }
  ```
  - Ignore import generation.
