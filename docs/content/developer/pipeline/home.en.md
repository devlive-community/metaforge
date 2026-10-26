---
title: Pipeline
---

In the MetaForge system, users can configure the pipeline function at will and adjust it arbitrarily according to the version of the low-level executor. The system will automatically identify and compile the final configuration according to the configuration and send it to the executor.

The configurations we can provide are:

|       Field        |     Type      |                           Description                           |
|:---------------:|:-----------:|:------------------------------------------------------:|
|     `field`     |  `String`   |                          Field name                           |
|    `origin`     |  `String`   | The default is equal to the field value, and the custom column name is used. If it is in `host|port` format, the system will splice the fields through `:`  |
|   `required`    |  `Boolean`  |                 When the value is `true`, it means that the field is required                 |
|   `override`    |  `Boolean`  | If this flag is `true`, it means that the field is extracted through user configuration, the default data will be discarded |
|     `input`     |  `Boolean`  |                        Whether it is an input parameter                         |
|     `width`     |  `Integer`  |                     Component width, default `300`                     |
|     `type`      | `FieldType` |                    Field type, default `INPUT`                    |
|    `tooltip`    |  `String`   |                          Prompt message                          |
|  `description`  |  `String`   |                          Description                          |
|     `value`     |  `Object`   |                       The result of the current configuration input                        |
|    `hidden`     |  `Boolean`  |            If this configuration item is `true`, the front end will not be displayed and will only be displayed after enabling it.             |
| `defaultValues` |   `Array`   |                If the type is SELECT , you need to pass in default data                 |

!!! danger

    We do not recommend users to modify the above configurations by themselves. If there are any configuration abnormalities, the plug-in will not be able to use this function.
!!!
