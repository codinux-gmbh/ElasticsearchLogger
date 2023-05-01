# Elasticsearch Logger

Implements logging to Elasticsearch for various logging frameworks.

Base for the Quarkus Elasticsearch Logger: https://github.com/codinux-gmbh/quarkus-elasticsearch-logger

## Features

- Logs directly to Elasticsearch
- Buffers messages and sends them asynchronously each 100 ms (configurable) in a bulk message to Elasticsearch to reduce round trips(?)
- Supports timestamps with nanoseconds resolution (if supported by system), see [Nanosecond timestamp precision](#nanosecond-timestamp-precision)
- Supports MDC, NDC and Marker (if supported by logging framework)
- For Kubernetes environments supports retrieving and logging pod and Kubernetes info
- Each field can be configured individually if it should be included in or excluded from sending to Elasticsearch
- All field names are configurable
- Supports index-per-day, e.g. by setting index name to log-%date{yyyy.MM.dd}

## Setup

### Logback

#### Gradle

```
implementation 'net.codinux.log:logback-elasticsearch-logger:2.1.0'
```

#### Maven

```
<dependency>
    <groupId>net.codinux.log</groupId>
    <artifactId>logback-elasticsearch-logger</artifactId>
    <version>2.1.0</version>
</dependency>
```

### JBoss Logging

#### Gradle

```
implementation 'net.codinux.log:jboss-logging-elasticsearch-logger:2.1.0'
```

#### Maven

```
<dependency>
    <groupId>net.codinux.log</groupId>
    <artifactId>jboss-logging-elasticsearch-logger</artifactId>
    <version>2.1.0</version>
</dependency>
```

### Java Util Logging

#### Gradle

```
implementation 'net.codinux.log:java-util-logging-elasticsearch-logger:2.1.0'
```

#### Maven

```
<dependency>
    <groupId>net.codinux.log</groupId>
    <artifactId>java-util-logging-elasticsearch-logger</artifactId>
    <version>2.1.0</version>
</dependency>
```

## Kubernetes info

ElasticsearchLogger can log information about the Pod and the Kubernetes environment. But in order for this to work the pod must be given the privilege to access the Kubernetes API:

[//]: # ( TODO)

## Nanosecond timestamp precision

Starting with Elasticsearch 7.0 and Kibana 7.5 timestamps with nanoseconds precision are supported by the new field type `date_nanos`.

(!) Important: For having nanosecond precision the type of the timestamp field has to be set before the first log is send to the index.
Elasticsearch's dynamic mapping for date fields is `date`, which only has milliseconds precisions.
The mapping of a field cannot be changed after the first record is indexed.

You have multiple options to create the correct mapping beforehand.
For all the following examples replace `http://localhost:9200` with the URL of your Elasticsearch instance, `my_app_logs` with the name of your log index and
`@timestamp` with the name of your timestamp field:

- Create the index with the correct mapping:

URL:

`PUT http://localhost:9200/my_app_logs`

Body:
```json
{
  "mappings": {
    "properties": {
      "@timestamp": {
        "type": "date_nanos"
      }
    }
  }
}
```

- Set an index template for all your log indices (suppose all index names start with `log-`)

URL:

`PUT http://localhost:9200/_template/log-indices-template`

Body:
```json
{
  "index_patterns": [ "log-*" ],
  "mappings": {
    "properties": {
      "@timestamp": {
        "type": "date_nanos"
      }
    }
  }
}
```

Additionally may also set:
```json
{
  "index_patterns": [ "log-*" ],
  "mappings" : {
    "properties" : {
      "@timestamp" : {
        "type" : "date_nanos"
      },
      "message" : {
        "type" : "text"
      }
    },
    "dynamic_templates" : [
      {
        "k8s" : {
          "match_mapping_type": "string",
          "mapping" : {
            "type" : "keyword"
          }
        }
      }
    ]
  }
}
```

# License

    Copyright 2021 codinux GmbH & Co. KG

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.