import gql from "graphql-tag";
import {ReferentialFragments} from "../../referential/referential.module";

export const Fragments = {
  referential: ReferentialFragments.referential,
  department: ReferentialFragments.department,
  lightDepartment: ReferentialFragments.lightDepartment,
  location: ReferentialFragments.location,
  metier: ReferentialFragments.metier,
  lightMetier: ReferentialFragments.lightMetier,
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
  position: gql`fragment PositionFragment on VesselPositionVO {
    id
    dateTime
    latitude
    longitude
    updateDate
    qualityFlagId
    recorderDepartment {
      id
      label
      name
      __typename
    }
    __typename
  }`,
  measurement: gql`fragment MeasurementFragment on MeasurementVO {
    id
    pmfmId
    alphanumericalValue
    numericalValue
    rankOrder
    qualitativeValue {
      id
      label
      name
      entityName
      __typename
    }
    digitCount
    qualityFlagId
    creationDate
    updateDate
    recorderDepartment {
      id
      label
      name
      __typename
    }
    entityName
    __typename
  }`
};

export const DataFragments = {
  sample: gql`fragment SampleFragment on SampleVO {
    id
    label
    rankOrder
    parentId
    sampleDate
    individualCount
    size
    sizeUnit
    comments
    updateDate
    creationDate
    matrix {
      ...ReferentialFragment
    }
    taxonName {
      ...ReferentialFragment
    }
    taxonGroup {
      ...ReferentialFragment
    }
    measurementValues
    __typename
  }
  ${Fragments.referential}`,
  batch: gql`fragment BatchFragment on BatchVO {
    id
    label
    rankOrder
    parentId
    exhaustiveInventory
    samplingRatio
    samplingRatioText
    individualCount
    comments
    updateDate
    taxonGroup {
      ...ReferentialFragment
    }
    taxonName {
      ...TaxonNameFragment
    }
    measurementValues
    __typename
  }
  ${Fragments.referential}
  ${ReferentialFragments.taxonName}`
};
