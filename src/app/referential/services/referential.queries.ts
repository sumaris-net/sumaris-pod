import gql from "graphql-tag";

export const ReferentialFragments = {
  referential: gql`fragment ReferentialFragment on ReferentialVO {
    id
    label
    name
    entityName
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
  }`
};
