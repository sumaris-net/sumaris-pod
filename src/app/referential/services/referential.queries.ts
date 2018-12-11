import gql from "graphql-tag";

export const ReferentialFragments = {
  referential: gql`
      fragment ReferentialFragment on ReferentialVO {
        id
        label
        name
        entityName
        __typename
      }
    `
};
