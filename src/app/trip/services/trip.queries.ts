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
    taxonGroup {
      ...ReferentialFragment
    }
    taxonName {
      ...TaxonNameFragment
    }
    measurementValues
    qualityFlagId
    __typename
  }
  ${Fragments.referential}
  ${ReferentialFragments.taxonName}`,
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
    qualityFlagId
    __typename
  }
  ${Fragments.referential}
  ${ReferentialFragments.taxonName}`,
  product: gql`fragment ProductFragment on ProductVO {
    id
    label
    rankOrder
    individualCount
    subgroupCount
    weight
    weightMethod {
      ...ReferentialFragment
    }
    comments
    updateDate
    taxonGroup {
      ...ReferentialFragment
    }
    saleType {
      ...ReferentialFragment
    }
    measurementValues
    quantificationMeasurements {
      ...MeasurementFragment
    }
    sortingMeasurements {
      ...MeasurementFragment
    }
    qualityFlagId
    operationId
    saleId
    landingId
    batchId
    __typename
  }
  ${Fragments.referential}
  ${Fragments.measurement}
  `
};

export const PhysicalGearFragments = {
  physicalGear: gql`fragment PhysicalGearFragment on PhysicalGearVO {
  id
  rankOrder
  updateDate
  creationDate
  comments
  gear {
    ...ReferentialFragment
  }
  recorderDepartment {
    ...LightDepartmentFragment
  }
  measurementValues
}
`};


export const OperationGroupFragment = {
  operationGroup: gql`fragment OperationGroupFragment on OperationGroupVO {
    id
    rankOrderOnPeriod
    physicalGearId
    tripId
    comments
    hasCatch
    updateDate
    metier {
      ...MetierFragment
    }
    physicalGear {
      ...PhysicalGearFragment
    }
    recorderDepartment {
      ...LightDepartmentFragment
    }
    measurements {
      ...MeasurementFragment
    }
    gearMeasurements {
      ...MeasurementFragment
    }
    batches {
      ...BatchFragment
    }
    products {
      ...ProductFragment
    }
  }
  ${ReferentialFragments.lightDepartment}
  ${ReferentialFragments.metier}
  ${DataFragments.batch}
  ${DataFragments.product}
  ${PhysicalGearFragments.physicalGear}
  ${Fragments.measurement}
  `
};

export const SaleFragments = {
  lightSale: gql`fragment LightSaleFragment on SaleVO {
    id
    startDateTime
    creationDate
    updateDate
    comments
    saleType {
      ...ReferentialFragment
    }
    saleLocation {
      ...LocationFragment
    }
  }
  ${Fragments.referential}
  ${Fragments.location}
  `,
  sale: gql`fragment SaleFragment on SaleVO {
    id
    startDateTime
    creationDate
    updateDate
    comments
    saleType {
      ...ReferentialFragment
    }
    saleLocation {
      ...LocationFragment
    }
    measurements {
      ...MeasurementFragment
    }
    products {
      ...ProductFragment
    }
  }
  ${Fragments.referential}
  ${Fragments.location}
  ${Fragments.measurement}
  ${DataFragments.product}
  `
};

