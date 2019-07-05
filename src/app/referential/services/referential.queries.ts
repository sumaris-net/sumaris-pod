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
};
