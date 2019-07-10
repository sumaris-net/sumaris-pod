import gql from "graphql-tag";
import {ReferentialFragments} from "../../referential/referential.module";

export const Fragments = {
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
  referential: ReferentialFragments.referential,
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
  ${ReferentialFragments.taxonName}`,
  vesselFeatures: gql`fragment VesselFeaturesFragment on VesselFeaturesVO {
    id
    vesselId
    name
    exteriorMarking
  }
  `
};
