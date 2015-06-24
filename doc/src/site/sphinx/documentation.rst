Stratio's Cassandra Lucene Index
++++++++++++++++++++++++++++++++

Overview
********

`Cassandra <http://cassandra.apache.org/>`__ index functionality has
been extended to provide near real time search such as
`ElasticSearch <http://www.elasticsearch.org/>`__ or
`Solr <https://lucene.apache.org/solr/>`__, including full text search
capabilities and multivariable and geospatial search.

It is also fully compatible with `Apache
Spark <https://spark.apache.org/>`__ and `Apache
Hadoop <https://hadoop.apache.org/>`__, allowing you to filter data at
database level. This speeds up jobs reducing the amount of data to be
collected and processed.

Indexing is achieved through a Lucene based implementation of Cassandra
secondary indexes, where each node of the cluster indexes its own data.
Stratio Cassandra is one of the core modules on which Stratio's BigData
platform (SDS) is based.

Features
========

Lucene search technology integration into Cassandra provides:

-  Full text search
-  Geospatial search
-  Date ranges (durations) search
-  Multidimensional boolean (and, or, not) search
-  Near real-time search
-  Relevance scoring and sorting
-  General top-k queries
-  Custom analyzers
-  CQL3 support
-  Wide rows support
-  Partition and cluster composite keys support
-  Support for indexing columns part of primary key
-  Third-party drivers compatibility
-  Spark compatibility
-  Hadoop compatibility
-  Paging over non-relevance queries

Not yet supported:

-  Thrift API
-  Legacy compact storage option
-  Indexing ``counter`` columns
-  Columns with TTL
-  CQL user defined types
-  Static columns
-  Paging over relevance queries

Requirements
============

-  Cassandra 2.1.6
-  Java >= 1.7 (OpenJDK and Sun have been tested)
-  Maven >= 3.0

Installation
============

Stratio's Cassandra Lucene Index is distributed as a plugin for Apache
Cassandra. Thus, you just need to build a JAR containing the plugin and
add it to the Cassandra's classpath:

-  Build the plugin with Maven: ``mvn clean package``
-  Copy the generated JAR to the lib folder of your comaptible Cassandra
   installation:
   ``cp target/cassandra-lucene-index-2.1.6.0.jar <CASSANDRA_HOME>/lib/``
-  Start/restart Cassandra as usual

Patching can also be done with this Maven profile, specifying the path
of your Cassandra installation:

.. code-block:: bash

    mvn clean package -Ppatch -Dcassandra_home=<CASSANDRA_HOME>

Alternatively, if you don't have an installed version of Cassandra,
there is a profile to let Maven download and patch the proper version of
Apache Cassandra:

.. code-block:: bash

    mvn clean package -Pdownload_and_patch -Dcassandra_home=<CASSANDRA_HOME>

Now you can run Cassandra and do some tests using the Cassandra Query
Language:

.. code-block:: bash

    <CASSANDRA_HOME>/bin/cassandra -f
    <CASSANDRA_HOME>/bin/cqlsh

The Lucene's index files will be stored in the same directories where
the Cassandra's will be. The default data directory is
``/var/lib/cassandra/data``, and each index is placed next to the
SSTables of its indexed column family.

For more details about Apache Cassandra please see its
`documentation <http://cassandra.apache.org/>`__.

Example
=======

We will create the following table to store tweets:

.. code-block:: sql

    CREATE KEYSPACE demo
    WITH REPLICATION = {'class' : 'SimpleStrategy', 'replication_factor': 1};
    USE demo;
    CREATE TABLE tweets (
        id INT PRIMARY KEY,
        user TEXT,
        body TEXT,
        time TIMESTAMP,
        latitude FLOAT,
        longitude FLOAT,
        lucene TEXT
    );

We have created a column called *lucene* to link the index queries. This column will not store data. Now you can create a custom Lucene index on it with the following statement:

.. code-block:: sql

    CREATE CUSTOM INDEX tweets_index ON tweets (lucene)
    USING 'com.stratio.cassandra.lucene.Index'
    WITH OPTIONS = {
        'refresh_seconds' : '1',
        'schema' : '{
            fields : {
                id    : {type : "integer"},
                user  : {type : "string"},
                body  : {type : "text", analyzer : "english"},
                time  : {type : "date", pattern : "yyyy/MM/dd"},
                place : {type : "geo_point", latitude:"latitude", longitude:"longitude"}
            }
        }'
    };

This will index all the columns in the table with the specified types, and it will be refreshed once per second.

Now, to query the top 100 more relevant tweets where *body* field contains the phrase "big data gives organizations":

.. code-block:: sql

    SELECT * FROM tweets WHERE lucene='{
        query : {type:"phrase", field:"body", value:"big data gives organizations", slop:1}
    }' limit 100;

To restrict the search for tweets within a certain date range, then you must add to the search a filter as follows:

.. code-block:: sql

    SELECT * FROM tweets WHERE lucene='{
        filter : {type:"range", field:"time", lower:"2014/04/25", upper:"2014/05/1"},
        query  : {type:"phrase", field:"body", value:"big data gives organizations", slop:1}
    }' limit 100;

To refine the search to get only the tweets written by users whose name starts with "a":

.. code-block:: sql

    SELECT * FROM tweets WHERE lucene='{
        filter : {type:"boolean", must:[
                       {type:"range", field:"time", lower:"2014/04/25", upper:"2014/05/1"},
                       {type:"prefix", field:"user", value:"a"} ] },
        query  : {type:"phrase", field:"body", value:"big data gives organizations", slop:1}
    }' limit 100;

To get the 100 more recent filtered results you can use the *sort* option:

.. code-block:: sql

    SELECT * FROM tweets WHERE lucene='{
        filter : {type:"boolean", must:[
                       {type:"range", field:"time", lower:"2014/04/25", upper:"2014/05/1"},
                       {type:"prefix", field:"user", value:"a"} ] },
        query  : {type:"phrase", field:"body", value:"big data gives organizations", slop:1},
        sort  : {fields: [ {field:"time", reverse:true} ] }
    }' limit 100;

The previous query can be restricted to a geographical bounding box:

.. code-block:: sql

    SELECT * FROM tweets WHERE lucene='{
        filter : {type:"boolean", must:[
                       {type:"range", field:"time", lower:"2014/04/25", upper:"2014/05/1"},
                       {type:"prefix", field:"user", value:"a"},
                       {type:"geo_bbox",
                        field:"place",
                        min_latitude:40.225479,
                        max_latitude:40.560174,
                        min_longitude:-3.999278,
                        max_longitude:-3.378550} ] },
        query  : {type:"phrase", field:"body", value:"big data gives organizations", slop:1},
        sort  : {fields: [ {field:"time", reverse:true} ] }
    }' limit 100;

Alternatively, you can restrict the query to retrieve tweets that are within a specific distance from a geographical position:

.. code-block:: sql

    SELECT * FROM tweets WHERE lucene='{
        filter : {type:"boolean", must:[
                       {type:"range", field:"time", lower:"2014/04/25", upper:"2014/05/1"},
                       {type:"prefix", field:"user", value:"a"},
                       {type:"geo_distance",
                        field:"place",
                        latitude:40.393035,
                        longitude:-3.732859,
                        max_distance:"10km",
                        min_distance:"100m"} ] },
        query  : {type:"phrase", field:"body", value:"big data gives organizations", slop:1},
        sort  : {fields: [ {field:"time", reverse:true} ] }
    }' limit 100;

Indexing
********

Syntax:

.. code-block:: sql

    CREATE CUSTOM INDEX (IF NOT EXISTS)? <index_name>
                                      ON <table_name> ( <magic_column> )
                                   USING 'com.stratio.cassandra.lucene.Index'
                            WITH OPTIONS = <options>

where:

-  <magic\_column> is the name of a text column that does not contain
   any data and will be used to show the scoring for each resulting row
   of a query,
-  <options> is a JSON object:

.. code-block:: sql

    <options> := { ('refresh_seconds'      : '<int_value>',)?
                   ('ram_buffer_mb'        : '<int_value>',)?
                   ('max_merge_mb'         : '<int_value>',)?
                   ('max_cached_mb'        : '<int_value>',)?
                   ('indexing_threads'     : '<int_value>',)?
                   ('indexing_queues_size' : '<int_value>',)?
                   'schema'                : '<schema_definition>'};

Options, except “schema”, take a positive integer value enclosed in
single quotes:

-  **refresh\_seconds**: number of seconds before refreshing the index
   (between writers and readers). Defaults to ’60’.
-  **ram\_buffer\_mb**: size of the write buffer. Its content will be
   committed to disk when full. Defaults to ’64’.
-  **max\_merge\_mb**: defaults to ’5’.
-  **max\_cached\_mb**: defaults to ’30’.
-  **indexing\_threads**: number of asynchronous indexing threads. ’0’
   means synchronous indexing. Defaults to ’0’.
-  **indexing\_queues\_size**: max number of queued documents per
   asynchronous indexing thread. Defaults to ’50’.
-  **schema**: see below

.. code-block:: sql

    <schema_definition> := {
        (analyzers : { <analyzer_definition> (, <analyzer_definition>)* } ,)?
        (default_analyzer : "<analyzer_name>",)?
        fields : { <field_definition> (, <field_definition>)* }
    }

Where default\_analyzer defaults to
‘org.apache.lucene.analysis.standard.StandardAnalyzer’.

.. code-block:: sql

    <analyzer_definition> := <analyzer_name> : {
        type : "<analyzer_type>" (, <option> : "<value>")*
    }

.. code-block:: sql

    <field_definition> := <column_name> : {
        type : "<field_type>" (, <option> : "<value>")*
    }

Analysis
========

Analyzer definition options depend on the analyzer type. Details and
default values are listed in the table below.

+-----------------+-------------+--------------+-----------------+
| Analyzer type   | Option      | Value type   | Default value   |
+=================+=============+==============+=================+
| classpath       | class       | string       | null            |
+-----------------+-------------+--------------+-----------------+
| snowball        | language    | string       | null            |
|                 +-------------+--------------+-----------------+
|                 | stopwords   | string       | null            |
+-----------------+-------------+--------------+-----------------+

Mapping
=======

Field mapping definition options depend on the field type. Details and
default values are listed in the table below.

+-----------------+-----------------+-----------------+--------------------------------+-----------+
| Mapper type     | Option          | Value type      | Default value                  | Mandatory |
+=================+=================+=================+================================+===========+
| bigdec          | indexed         | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | integer_digits  | integer         | 32                             | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | decimal_digits  | integer         | 32                             | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| bigint          | indexed         | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | digits          | integer         | 32                             | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| blob            | indexed         | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | true                           | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| boolean         | indexed         | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | true                           | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| date            | indexed         | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | pattern         | string          | yyyy/MM/dd HH:mm:ss.SSS        | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| date_range      | start           | string          |                                | Yes       |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | stop            | string          |                                | Yes       |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | pattern         | string          | yyyy/MM/dd HH:mm:ss.SSS        | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| double          | indexed         | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | boost           | integer         | 0.1f                           | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| float           | indexed         | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | boost           | integer         | 0.1f                           | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| geo_point       | latitude        | string          |                                | Yes       |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | longitude       | string          |                                | Yes       |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | max_levels      | integer         | 11                             | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| inet            | indexed         | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | true                           | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| integer         | indexed         | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | boost           | integer         | 0.1f                           | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| long            | indexed         | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | boost           | integer         | 0.1f                           | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| string          | indexed         | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | true                           | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| text            | indexed         | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | analyzer        | string          | default_analyzer of the schema | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+
| uuid            | indexed         | boolean         | true                           | No        |
+                 +-----------------+-----------------+--------------------------------+-----------+
|                 | sorted          | boolean         | true                           | No        |
+-----------------+-----------------+-----------------+--------------------------------+-----------+

All mapping definitions has an “\ **indexed**\ ” option indicating if
the field is searchable. There is also a “\ **sorted**\ ” option
specifying if it is possible to sort rows by the corresponding field.
Both fields are true by default, but they should be set to false when no
needed in order to have a smaller and faster index.

Note that Cassandra allows one custom index per table. On the other
hand, Cassandra does not allow a modify operation on indexes. To modify
an index it needs to be deleted first and created again.

Example
=======

This code below and the one for creating the corresponding keyspace and
table is available in a CQL script that can be sourced from the
Cassandra shell:
`test-users-create.cql <resources/test-users-create.cql>`__.

.. code-block:: sql

    CREATE CUSTOM INDEX IF NOT EXISTS users_index
    ON test.users (stratio_col)
    USING 'com.stratio.cassandra.lucene.Index'
    WITH OPTIONS = {
        'refresh_seconds'      : '1',
        'ram_buffer_mb'        : '64',
        'max_merge_mb'         : '5',
        'max_cached_mb'        : '30',
        'indexing_threads'     : '4',
        'indexing_queues_size' : '50',
        'schema' : '{
            analyzers : {
                  my_custom_analyzer : {
                      type:"snowball",
                      language:"Spanish",
                      stopwords : "el,la,lo,loas,las,a,ante,bajo,cabe,con,contra"}
            },
            default_analyzer : "english",
            fields : {
                name   : {type     : "string"},
                gender : {type     : "string", sorted: "false"},
                animal : {type     : "string"},
                age    : {type     : "integer"},
                food   : {type     : "string"},
                number : {type     : "integer"},
                bool   : {type     : "boolean"},
                date   : {type     : "date", pattern  : "yyyy/MM/dd"},
                mapz   : {type     : "string", sorted: "false"},
                setz   : {type     : "string", sorted: "false"},
                listz  : {type     : "string"},
                phrase : {type     : "text", analyzer : "my_custom_analyzer"}
            }
        }'
    };

Querying
********

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table_name>
    WHERE <magic_column> = '{ (   query  : <query>  )?
                              ( , filter : <filter> )?
                              ( , sort   : <sort>   )?
                            }';

where <query> and <filter> are a JSON object:

.. code-block:: sql

    <query> := { type : <type> (, <option> : ( <value> | <value_list> ) )+ }

and <sort> is another JSON object:

.. code-block:: sql

        <sort> := { fields : <sort_field> (, <sort_field> )* }
        <sort_field> := { field : <field> (, reverse : <reverse> )? }

When searching by <query>, results are returned ***sorted by descending
relevance*** without pagination. The results will be located in the
column ‘stratio\_relevance’.

Filter types and options are the same as the query ones. The difference
with queries is that filters have no effect on scoring.

Sort option is used to specify the order in which the indexed rows will
be traversed. When sorting is used, the query scoring is delayed.

If no query or sorting options are specified then the results are
returned in the Cassandra’s natural order, which is defined by the
partitioner and the column name comparator.

Types of query and their options are summarized in the table below.
Details for each of them are available in individual sections and the
examples can be downloaded as a CQL script:
`extended-search-examples.cql <resources/extended-search-examples.cql>`__.

In addition to the options described in the table, all query types have
a “\ **boost**\ ” option that acts as a weight on the resulting score.

+----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| Query type                             | Option          | Value type      | Default value                  | Mandatory |
+========================================+=================+=================+================================+===========+
| `Boolean <#boolean-query>`__           | must            | query           |                                | No        |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | should          | query           |                                | No        |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | not             | query           |                                | No        |
+----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Contains <#contains-query>`__         | field           | string          |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | values          | array           |                                | Yes       |
+----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Date range <#date-range-query>`__     | field           | String          |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | start           | string/long     |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | stop            | string/long     |                                | Yes       |
+----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Fuzzy <#fuzzy-query>`__               | field           | string          |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | value           | string          |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | max_edits       | integer         | 2                              | No        |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | prefix_length   | integer         | 0                              | No        |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | max_expansions  | integer         | 50                             | No        |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | transpositions  | boolean         | true                           | No        |
+----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Geo bounding box <#geo-bbox-query>`__ | field           | String          |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | min_latitude    | double          |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | max_latitude    | double          |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | min_longitude   | double          |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | max_longitude   | double          |                                | Yes       |
+----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Geo distance <#geo-distance-query>`__ | field           | string          |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | latitude        | double          |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | longitude       | double          |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | max_distance    | string          |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | min_distance    | string          |                                | No        |
+----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Match <#match-query>`__               | field           | string          |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | value           | any             |                                | Yes       |
+----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Match all <#match-all-query>`__       |                 |                 |                                |           |
+----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Phrase <#phrase-query>`__             | field           | string          |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | value           | string          |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | slop            | integer         | 0                              | No        |
+----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Prefix <#prefix-query>`__             | field           | string          |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | value           | string          |                                | Yes       |
+----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Range <#range-query>`__               | field           | string          |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | lower           | any             |                                | No        |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | upper           | any             |                                | No        |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | include_lower   | boolean         | false                          | No        |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | include_upper   | boolean         | false                          | No        |
+----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Regexp <#regexp-query>`__             | field           | string          |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | value           | string          |                                | Yes       |
+----------------------------------------+-----------------+-----------------+--------------------------------+-----------+
| `Wildcard <#wildcard-query>`__         | field           | string          |                                | Yes       |
|                                        +-----------------+-----------------+--------------------------------+-----------+
|                                        | value           | string          |                                | Yes       |
+----------------------------------------+-----------------+-----------------+--------------------------------+-----------+

Boolean query
=============

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ query : {
                               type     : "boolean",
                               ( must   : [(query,)?] , )?
                               ( should : [(query,)?] , )?
                               ( not    : [(query,)?] , )? } }';

where:

-  **must**: represents the conjunction of queries: query1 AND query2
   AND … AND queryn
-  **should**: represents the disjunction of queries: query1 OR query12
   OR … OR queryn
-  **not**: represents the negation of the disjunction of queries:
   NOT(query1 OR query2 OR … OR queryn)

Since "not" will be applied to the results of a "must" or "should"
condition, it can not be used in isolation.

Example 1: will return rows where name ends with “a” AND food starts
with “tu”

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type : "boolean",
                            must : [{type : "wildcard", field : "name", value : "*a"},
                                    {type : "wildcard", field : "food", value : "tu*"}]}}';

Example 2: will return rows where food starts with “tu” but name does
not end with “a”

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type : "boolean",
                            not  : [{type : "wildcard", field : "name", value : "*a"}],
                            must : [{type : "wildcard", field : "food", value : "tu*"}]}}';

Example 3: will return rows where name ends with “a” or food starts with
“tu”

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type   : "boolean",
                            should : [{type : "wildcard", field : "name", value : "*a"},
                                      {type : "wildcard", field : "food", value : "tu*"}]}}';

Contains query
==============

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ query : {
                                type  : "contains",
                                field : <fieldname> ,
                                values : <value_list> }}';

Example 1: will return rows where name matches “Alicia” or “mancha”

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type   : "contains",
                            field  : "name",
                            values : ["Alicia","mancha"] }}';

Example 2: will return rows where date matches “2014/01/01″,
“2014/01/02″ or “2014/01/03″

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type   : "contains",
                            field  : "date",
                            values : ["2014/01/01", "2014/01/02", "2014/01/03"] }}';

Date range query
================

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ query : {
                                type  : "contains",
                                start : <start> ,
                                stop  : <stop> ,
                                (, operation: <operation> )?
                              }}';

where:

-  **start**: a string or a number being the beginning of the date
   range.
-  **stop**: a string or a number being the end of the date range.
-  **operation**: the spatial operation to be performed, it can be
   **intersects**, **contains** and **is\_within**.

Example 1: will return rows where duration is within "2013/05/02" and
:"2013/05/03"

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                            type  : "date_range",
                            field : "duration",
                            start : "2013/05/02",
                            stop  : "2013/05/03",
                            operation : "is_within"}}';

Example 1: will return rows where duration intersects "2013/05/02" and
:"2013/05/03"

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{filter : {
                            type  : "date_range",
                            field : "duration",
                            start : "2013/05/02",
                            stop  : "2013/05/03",
                            operation : "intersects"}}';

Fuzzy query
===========

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ query : {
                                type  : "fuzzy",
                                field : <fieldname> ,
                                value : <value>
                                (, max_edits     : <max_edits> )?
                                (, prefix_length : <prefix_length> )?
                                (, max_expansions: <max_expansion> )?
                                (, transpositions: <transposition> )?
                              }}';

where:

-  **max\_edits** (default = 2): a integer value between 0 and 2. Will
   return rows which distance from <value> to <field> content has a
   distance of at most <max\_edits>. Distance will be interpreted
   according to the value of “transpositions”.
-  **prefix\_length** (default = 0): an integer value being the length
   of the common non-fuzzy prefix
-  **max\_expansions** (default = 50): an integer for the maximum number
   of terms to match
-  **transpositions** (default = true): if transpositions should be
   treated as a primitive edit operation (`Damerau-Levenshtein
   distance <http://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance>`__).
   When false, comparisons will implement the classic `Levenshtein
   distance <http://en.wikipedia.org/wiki/Levenshtein_distance>`__.

Example 1: will return any rows where “phrase” contains a word that
differs in one edit operation from “puma”, such as “pumas”.

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : { type      : "fuzzy",
                                    field     : "phrase",
                                    value     : "puma",
                                    max_edits : 1 }}';

Example 2: same as example 1 but will limit the results to rows where
phrase contains a word that starts with “pu”.

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : { type          : "fuzzy",
                                    field         : "phrase",
                                    value         : "puma",
                                    max_edits     : 1,
                                    prefix_length : 2 }}';

Geo bbox query
==============

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ query : {
                                type  : "geo_bbox",
                                field : <fieldname>,
                                min_latitude : <min_latitude> ,
                                max_latitude : <max_latitude> ,
                                min_longitude : <min_longitude> ,
                                max_longitude : <max_longitude>
                              }}';

where:

-  **min\_latitude** : a double value between -90 and 90 being the min
   allowed latitude.
-  **max\_latitude** : a double value between -90 and 90 being the max
   allowed latitude.
-  **min\_longitude** : a double value between -180 and 180 being the
   min allowed longitude.
-  **max\_longitude** : a double value between -180 and 180 being the
   max allowed longitude.

Example 1: will return any rows where “place” is formed by a latitude
between 40.225479 and 40.560174, and a longitude between -3.999278 and
-3.378550.

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : { type : "geo_bbox",
                                    field : "place",
                                    min_latitude : 40.225479,
                                    max_latitude : 40.560174,
                                    min_longitude : -3.999278,
                                    max_longitude : -3.378550 }}';

Geo distance query
==================

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ query : {
                                type  : "geo_distance",
                                field : <fieldname> ,
                                latitude : <latitude> ,
                                longitude : <longitude> ,
                                max_distance : <max_distance>
                                (, min_distance : <min_distance> )?
                              }}';

where:

-  **latitude** : a double value between -90 and 90 being the latitude
   of the reference point.
-  **longitude** : a double value between -180 and 180 being the
   longitude of the reference point.
-  **max\_distance** : a string value being the max allowed distance
   from the reference point.
-  **min\_distance** : a string value being the min allowed distance
   from the reference point.

Example 1: will return any rows where “place” is within one kilometer
from the geo point (40.225479, -3.999278).

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : { type : "geo_distance",
                                    field : "place",
                                    latitude : 40.225479,
                                    longitude : -3.999278,
                                    max_distance : "1km" }}';

Example 2: will return any rows where “place” is within one yard and ten
yards from the geo point (40.225479, -3.999278).

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : { type : "geo_distance",
                                    field : "place",
                                    latitude : 40.225479,
                                    longitude : -3.999278,
                                    max_distance : "10yd" ,
                                    min_distance : "1yd" }}';

Match query
===========

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ query : {
                                type  : "match",
                                field : <fieldname> ,
                                value : <value> }}';

Example 1: will return rows where name matches “Alicia”

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type  : "match",
                            field : "name",
                            value : "Alicia" }}';

Example 2: will return rows where phrase contains “mancha”

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type  : "match",
                            field : "phrase",
                            value : "mancha" }}';

Example 3: will return rows where date matches “2014/01/01″

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type  : "match",
                            field : "date",
                            value : "2014/01/01" }}';

Match all query
===============

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ query : {
                                type  : "match_all",
                                field : <fieldname> ,
                                value : <value> }}';

Example: will return all the indexed rows

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type  : "match_all" }}';

Phrase query
============

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ query : {
                                type  :"phrase",
                                field : <fieldname> ,
                                value : <value>
                                (, slop : <slop> )?
                            }}';

where:

-  **values**: an ordered list of values.
-  **slop** (default = 0): number of words permitted between words.

Example 1: will return rows where “phrase” contains the word “camisa”
followed by the word “manchada”.

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type   : "phrase",
                            field  : "phrase",
                            values : "camisa manchada" }}';

Example 2: will return rows where “phrase” contains the word “mancha”
followed by the word “camisa” having 0 to 2 words in between.

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type   : "phrase",
                            field  : "phrase",
                            values : "mancha camisa",
                            slop   : 2 }}';

Prefix query
============

Syntax:

.. code-block:: sql

    SELECT ( <fields> | * )
    FROM <table>
    WHERE <magic_column> = '{ query : {
                                type  : "prefix",
                                field : <fieldname> ,
                                value : <value> }}';

Example: will return rows where “phrase” contains a word starting with
“lu”. If the column is indexed as “text” and uses an analyzer, words
ignored by the analyzer will not be retrieved.

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type  : "prefix",
                            field : "phrase",
                            value : "lu" }}';

Range query
===========

Syntax:

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type    : "range",
                            field   : <fieldname>
                            (, lower : <min> , include_lower : <min_included> )?
                            (, upper : <max> , include_upper : <max_included> )?
                         }}';

where:

-  **lower**: lower bound of the range.
-  **include\_lower** (default = false): if the lower bound is included
   (left-closed range).
-  **upper**: upper bound of the range.
-  **include\_upper** (default = false): if the upper bound is included
   (right-closed range).

Lower and upper will default to :math:`-/+\\infty` for number. In the
case of byte and string like data (bytes, inet, string, text), all
values from lower up to upper will be returned if both are specified. If
only “lower” is specified, all rows with values from “lower” will be
returned. If only “upper” is specified then all rows with field values
up to “upper” will be returned. If both are omitted than all rows will
be returned.

Example 1: will return rows where *age* is in [1, ∞)

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type          : "range",
                            field         : "age",
                            lower         : 1,
                            include_lower : true }}';

Example 2: will return rows where *age* is in (-∞, 0]

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type          : "range",
                            field         : "age",
                            upper         : 0,
                            include_upper : true }}';

Example 3: will return rows where *age* is in [-1, 1]

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type          : "range",
                            field         : "age",
                            lower         : -1,
                            upper         : 1,
                            include_lower : true,
                            include_upper : true }}';

Example 4: will return rows where *date* is in [2014/01/01, 2014/01/02]

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type          : "range",
                            field         : "date",
                            lower         : "2014/01/01",
                            upper         : "2014/01/02",
                            include_lower : true,
                            include_upper : true }}';

Regexp query
============

Syntax:

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type  : "regexp",
                            field : <fieldname>,
                            value : <regexp>
                         }}';

where:

-  **value**: a regular expression. See
   `org.apache.lucene.util.automaton.RegExp <http://lucene.apache.org/core/4_6_1/core/org/apache/lucene/util/automaton/RegExp.html>`__
   for syntax reference.

Example: will return rows where name contains a word that starts with
“p” and a vowel repeated twice (e.g. “pape”).

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type  : "regexp",
                            field : "name",
                            value : "[J][aeiou]{2}.*" }}';

Wildcard query
==============

Syntax:

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type    : "wildcard" ,
                            field   : <fieldname> ,
                            value   : <wildcard_exp>
                         }}';

where:

-  **value**: a wildcard expression. Supported wildcards are \*, which
   matches any character sequence (including the empty one), and ?,
   which matches any single character. ” is the escape character.

Example: will return rows where food starts with or is “tu”.

.. code-block:: sql

    SELECT * FROM test.users
    WHERE stratio_col = '{query : {
                            type  : "wildcard",
                            field : "food",
                            value : "tu*" }}';

Spark and Hadoop
****************

Spark and Hadoop integrations are fully supported because Lucene queries
can be combined with token range queries and pagination, which are the
basis of MapReduce frameworks support.

Token Range Queries
===================

The token function allows computing the token for a given partition key.
The primary key of the example table “users” is ((name, gender), animal,
age) where (name, gender) is the partition key. When combining the token
function and a Lucene-based filter in a where clause, the filter on
tokens is applied first and then the condition of the filter clause.

Example: will retrieve rows which tokens are greater than (‘Alicia’,
‘female’) and then test them against the match condition.

.. code-block:: sql

    SELECT name,gender
      FROM test.users
     WHERE stratio_col='{filter : {type : "match", field : "food", value : "chips"}}'
       AND token(name, gender) > token('Alicia', 'female');

Pagination
==========

Pagination over filtered results is fully supported. You can retrieve
the rows starting from a certain key. For example, if the primary key is
(userid, createdAt), you can query:

.. code-block:: sql

    SELECT *
      FROM tweets
      WHERE stratio_col = ‘{ filter : {type:”match",  field:”text", value:”cassandra”} }’
        AND userid = 3543534
        AND createdAt > 2011-02-03 04:05+0000
      LIMIT 5000;

Note that paging does not support neither relevance queries nor sorting. You must increase the page size until the number of desired results.