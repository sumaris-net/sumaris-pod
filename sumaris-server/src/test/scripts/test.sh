#!/bin/sh

curl -XPOST 'http://localhost:7777/graphql' -H "Content-Type: application/json" -d '
{
"operationName": null,
    "variables": {},
  "query":"{\n  trips {\n    id\n    departureDateTime\n    returnDateTime\n    comments\n    creationDate\n    __typename\n  }\n}"
}'