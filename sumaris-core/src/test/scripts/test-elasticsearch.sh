#!/bin/bash

NODE_PORT=9200
NODE_URI="http://localhost:${NODE_PORT}"
INDEX=vessel_snapshot

curl -XPOST "${NODE_URI}/${INDEX}/_search?pretty&_source=name" -H 'Content-Type: application/json' -d'
{
     "size": 10,
     "query": {
                "bool" : {
                  "minimum_should_match": 2,
                  "should" : [
                      {
                        "wildcard" : {
                          "name" : "*we*"
                        }
                      },
                      {
                        "match" : {
                          "name" : "drez"
                        }
                      }
                    ]
                }
              }
}';