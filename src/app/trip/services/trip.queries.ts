import gql from "graphql-tag";

export const Fragments = {
  department: gql`
      fragment DepartmentFragment on DepartmentVO {
        id
        label
        name
        logo
      }
    `,
  person: gql`
      fragment PersonFragment on PersonVO {
        id
        firstName
        lastName
        avatar
        department {
          id
          label
          name
        }
      }
    `,
  location: gql`
      fragment LocationFragment on LocationVO {
        id
        label
        name
        entityName
      }
    `,
  referential: gql`
      fragment ReferentialFragment on ReferentialVO {
        id
        label
        name
        entityName
      }
    `,
  position: gql`
      fragment PositionFragment on VesselPositionVO {
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
        }
      }
    `,
  measurement: gql`
      fragment MeasurementFragment on MeasurementVO {
        id
        pmfmId
        alphanumericalValue
        numericalValue
        qualitativeValue {
          id
          label
        }
        digitCount
        rankOrder
        qualityFlagId
        creationDate
        updateDate
        recorderDepartment {
          id
          label
          name
        }
        entityName
      }
    `
};
