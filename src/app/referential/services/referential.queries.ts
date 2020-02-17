import gql from "graphql-tag";

export const ReferentialFragments = {
  referential: gql`fragment ReferentialFragment on ReferentialVO {
    id
    label
    name
    statusId
    entityName
    __typename
  }`,
  fullReferential: gql`fragment FullReferentialFragment on ReferentialVO {
    id
    label
    name
    description
    comments
    updateDate
    creationDate
    statusId
    validityStatusId
    levelId
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
  lightDepartment: gql`fragment LightDepartmentFragment on DepartmentVO {
    id
    label
    name
    logo
    __typename
  }`,
  lightPerson: gql`fragment LightPersonFragment on PersonVO {
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
    statusId
    levelId
    referenceTaxonId
    entityName
    isReferent
    __typename
  }`,
  fullTaxonName: gql`fragment FullTaxonNameFragment on TaxonNameVO {
    id
    label
    name
    statusId
    levelId
    referenceTaxonId
    entityName
    isReferent
    taxonGroupIds
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
  }`,
  pmfm: gql`fragment PmfmFragment on PmfmVO {
    id
    label
    name
    minValue
    maxValue
    unit
    defaultValue
    maximumNumberDecimals
    unitId
    parameterId
    matrixId
    fractionId
    methodId
    levelId: parameterId
    __typename
  }`,
};
