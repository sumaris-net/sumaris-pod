#!/bin/bash

NODE_PORT=9200
NODE_URI="http://localhost:${NODE_PORT}"
INDEX=vessel_snapshot


curl -XPOST "${NODE_URI}/${INDEX}/_search?pretty" -H 'Content-Type: application/json' -d'
{
     "size": 10,
     "query": {
        "nested": {
           "path": "program",
           "query": {
               "match_all": {}
           }
        }
     }
}';

exit 0

curl -XPOST "${NODE_URI}/${INDEX}/_search?pretty" -H 'Content-Type: application/json' -d'
{
     "size": 10,
     "query": {
                "bool" : {
                  "must" : [
                    {
                      "terms" : {
                        "vesselStatusId" : [
                          1,
                          2
                        ],
                        "boost" : 1.0
                      }
                    },

                     {
                        "nested" : {
                          "query" : {
                            "term" : {
                              "program.label" : {
                                "value" : "SIH",
                                "boost" : 1.0
                              }
                            }
                          },
                          "path" : "program",
                          "ignore_unmapped" : false,
                          "score_mode" : "none",
                          "boost" : 1.0
                        }
                      },

                    {
                      "nested" : {
                        "query" : {
                          "terms" : {
                            "vesselType.id" : [
                              1,
                              2
                            ],
                            "boost" : 1.0
                          }
                        },
                        "path" : "vesselType",
                        "ignore_unmapped" : false,
                        "score_mode" : "none",
                        "boost" : 1.0
                      }
                    },
                    {
                                          "bool" : {
                                            "should" : [
                                              {
                                                "wildcard" : {
                                                  "name" : {
                                                    "wildcard" : "*navire*",
                                                    "boost" : 1.0
                                                  }
                                                }
                                              }
                                            ],
                                            "adjust_pure_negative" : true,
                                            "minimum_should_match" : "1",
                                            "boost" : 1.0
                                          }
                                        }
                  ],

                  "must_not" : [
                                      {
                                        "bool" : {
                                          "should" : [
                                            {
                                              "range" : {
                                                "endDate" : {
                                                  "from" : null,
                                                  "to" : 1698150083722,
                                                  "include_lower" : true,
                                                  "include_upper" : false,
                                                  "boost" : 1.0
                                                }
                                              }
                                            },
                                            {
                                              "range" : {
                                                "startDate" : {
                                                  "from" : 1698150083722,
                                                  "to" : null,
                                                  "include_lower" : false,
                                                  "include_upper" : true,
                                                  "boost" : 1.0
                                                }
                                              }
                                            }
                                          ],
                                          "adjust_pure_negative" : true,
                                          "boost" : 1.0
                                        }
                                      }
                                    ],

                  "adjust_pure_negative" : true,
                  "boost" : 1.0
                }
              }
}';