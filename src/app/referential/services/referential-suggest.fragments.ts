import gql from "graphql-tag";

export const ReferentialSuggestFragments = {
  suggestedStrategyNextLabel:  gql`fragment SuggestedStrategyNextLabelFragment on String {
    id
    label
    name
    entityName
    __typename
  }`,
};
