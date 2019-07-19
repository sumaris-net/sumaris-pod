import gql from "graphql-tag";

export const ReferentialFragments = {
  referential: gql`fragment ReferentialFragment on ReferentialVO {
    id
    label
    name
    entityName
    __typename
  }`,
  department: gql`fragment DepartmentFragment on DepartmentVO {
    id
    label
    name
    logo
    __typename
  }`,
  recorderDepartment: gql`fragment RecorderDepartmentFragment on DepartmentVO {
    id
    label
    name
    logo
    __typename
  }`,
  recorderPerson: gql`fragment RecorderPersonFragment on PersonVO {
    id
    firstName
    lastName
    avatar
    department {
      id
      label
      name
      __typename
    }
    __typename
  }`,
  location: gql`fragment LocationFragment on LocationVO {
    id
    label
    name
    entityName
    __typename
  }`,
  taxonName: gql`fragment TaxonNameFragment on TaxonNameVO {
    id
    label
    name
    entityName
    referenceTaxonId
    __typename
  }`,
  taxonGroup: gql`fragment TaxonGroupFragment on TaxonGroupVO {
    id
    label
    name
    entityName
    taxonNames {
      ...TaxonNameFragment
    }
    __typename
  }`,
  lightMetier:  gql`fragment LightMetierFragment on MetierVO {
    id
    label
    name
    entityName
    __typename
  }`,
  metier:  gql`fragment MetierFragment on MetierVO {
    id
    label
    name
    entityName
    taxonGroup {
      id
      label
      name
      entityName
      __typename
    }
    gear {
      id
      label
      name
      entityName
      __typename
    }
    __typename
  }`
};
