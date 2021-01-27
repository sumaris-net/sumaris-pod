import {gql} from "@apollo/client/core";

export const StrategyFragments = {
  strategy: gql`fragment StrategyFragment on StrategyVO {
    id
    label
    name
    description
    comments
    analyticReference
    updateDate
    creationDate
    statusId
    programId
    gears {
      ...ReferentialFragment
    }
    taxonGroups {
      ...TaxonGroupStrategyFragment
    }
    taxonNames {
      ...TaxonNameStrategyFragment
    }
    appliedStrategies {
      ...AppliedStrategyFragment
    }
    pmfmStrategies {
      ...PmfmStrategyFragment
    }
    departments {
      ...StrategyDepartmentFragment
    }
  }
  `,
  appliedStrategy: gql`
    fragment AppliedStrategyFragment on AppliedStrategyVO {
      id
      strategyId
      location {
        ...ReferentialFragment
      }
      appliedPeriods {
        ...AppliedPeriodFragment
      }
      __typename
    }
  `,
  appliedPeriod: gql`
    fragment AppliedPeriodFragment on AppliedPeriodVO {
      appliedStrategyId
      startDate
      endDate
      acquisitionNumber
      __typename
    }
  `,
  strategyDepartment: gql`
    fragment StrategyDepartmentFragment on StrategyDepartmentVO {
      id
      strategyId
      location {
        ...ReferentialFragment
      }
      privilege {
        ...ReferentialFragment
      }
      department {
        ...ReferentialFragment
      }
      __typename
    }
  `,
  pmfmStrategy: gql`
    fragment PmfmStrategyFragment on PmfmStrategyVO {
      id
      acquisitionLevel
      rankOrder
      isMandatory
      acquisitionNumber
      defaultValue
      pmfmId
      pmfm {
        ...PmfmFragment
      }
      parameterId
      parameter {
        ...ReferentialFragment
      }
      matrixId
      matrix {
        ...ReferentialFragment
      }
      fractionId
      fraction {
        ...ReferentialFragment
      }
      methodId
      method {
        ...ReferentialFragment
      }
      gearIds
      taxonGroupIds
      referenceTaxonIds
      strategyId
      __typename
    }`,
  taxonGroupStrategy: gql`
    fragment TaxonGroupStrategyFragment on TaxonGroupStrategyVO {
      strategyId
      priorityLevel
      taxonGroup {
        id
        label
        name
        entityName
        taxonNames {
          ...TaxonNameFragment
        }
      }
      __typename
    }
  `,
  taxonNameStrategy: gql`
    fragment TaxonNameStrategyFragment on TaxonNameStrategyVO {
      strategyId
      priorityLevel
      taxonName {
        ...TaxonNameFragment
      }
      __typename
    }
  `,
  strategyRef: gql`
    fragment StrategyRefFragment on StrategyVO {
      id
      label
      name
      description
      comments
      updateDate
      creationDate
      statusId
      gears {
        ...ReferentialFragment
      }
      taxonGroups {
        ...TaxonGroupStrategyFragment
      }
      taxonNames {
        ...TaxonNameStrategyFragment
      }
      pmfmStrategies {
        ...PmfmStrategyRefFragment
      }
    }
  `,
  pmfmStrategyRef: gql`
    fragment PmfmStrategyRefFragment on PmfmStrategyVO {
      id
      pmfmId
      parameterId # TODO BLA check if need
      matrixId
      fractionId
      methodId
      label
      name
      unitLabel
      type
      minValue
      maxValue
      maximumNumberDecimals
      defaultValue
      acquisitionNumber
      isMandatory
      rankOrder
      acquisitionLevel
      gearIds
      taxonGroupIds
      referenceTaxonIds
      qualitativeValues {
        id
        label
        name
        statusId
        entityName
        __typename
      }
      __typename
    }`
};
