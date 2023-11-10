# convention 4 kubernetes: c4k-common
[![Clojars Project](https://img.shields.io/clojars/v/org.domaindrivenarchitecture/c4k-common-clj.svg)](https://clojars.org/org.domaindrivenarchitecture/c4k-common-clj) [![Clojars Project](https://img.shields.io/clojars/v/org.domaindrivenarchitecture/c4k-common-cljs.svg)](https://clojars.org/org.domaindrivenarchitecture/c4k-common-cljs) [![pipeline status](https://gitlab.com/domaindrivenarchitecture/c4k-common/badges/master/pipeline.svg)](https://gitlab.com/domaindrivenarchitecture/c4k-common/-/commits/master) 

[<img src="https://domaindrivenarchitecture.org/img/delta-chat.svg" width=20 alt="DeltaChat"> chat over e-mail](mailto:buero@meissa-gmbh.de?subject=community-chat) | [<img src="https://meissa-gmbh.de/img/community/Mastodon_Logotype.svg" width=20 alt="team@social.meissa-gmbh.de"> team@social.meissa-gmbh.de](https://social.meissa-gmbh.de/@team) | [Website & Blog](https://domaindrivenarchitecture.org)

## Purpose

c4k-common provides the foundation for all our c4k modules.

It is now possible to generate a working prometheus monitoring file in yaml format.
Your config.edn and your auth.edn should at least contain the following fields:

config.edn - minimal example

```clojure
{:k3s-cluster-name "your-cluster-name"
 :k3s-cluster-stage :prod
 :grafana-cloud-url "your-url"}
```  

auth.edn - minimal example  

```clojure
{:grafana-cloud-user "user"
 :grafana-cloud-password "password"}
```  

call (with jarwrapper installed):  

```bash
c4k-common-standalone.jar config.edn auth.edn > monitoring.yaml
```


## Rationale

There are many comparable solutions for creating c4k deployments like helm or kustomize. Why do we need another one?
* We like the simplicity of kustomize. Yaml in, yaml out, the ability to lint the result and the option to split large yaml files into objects. But a simple overwriting per environment may not be enough ...
* We like helm packages. A package encapsulates the setup for an application. On the one hand, but on the other hand we don't like the idea of having to program and debug in a template language. We can program much better in real programming languages.

Our convention 4 kubernetes c4k-* tools combine the advantages of both approaches:
* Packages for one application
* Programming in clojure
* yaml / edn as input and output, no more magic
* good validation, integration as api, cli or in the browser

## Usage

c4k-common provides the basic functionality for our c4k-modules.

## Refactoring & Module Overview

<!--- 
1. version 
2. configs as EDN and YAML
3. renamed test-helper
4. common load-as-edn
5. standardized uberjar
6. groups for webview
7. use common ingress
-->

| Module        | Version | [common load-as-edn][edn1] | [groups for webview][bgrp1] | [use common ingress][ing1] | [use common monitoring][mon1] | [validate examples][val1] | [ci with pyb][cipyb] | [inline-macro to load resources][macro] |
|---------------|---------|:--------------------------:|:---------------------------:|:--------------------------:|:-----------------------------:|:-------------------------:|:--------------------:|:---------------------------------------:|
| c4k-keycloak  | 0.2     |             x              |              x              |             x              |               x               |             x             |                      |                                         |
| c4k-taiga     | 0.1     |                            |                             |                            |                               |                           |                      |                                         |
| c4k-nextcloud | 4.0     |             x              |              x              |             x              |               x               |             x             |                      |                                         |
| c4k-jitsi     | 1.6     |             x              |              x              |             x              |               x               |             x             |          x           |                    x                    |
| c4k-forgejo   | 2.0     |             x              |              x              |             x              |               x               |             x             |                      |                                         |
| c4k-shynet    | 1.0     |                            |                             |                            |                               |                           |                      |                                         |
| c4k-website   | 1.1     |             x              |              x              |             x              |               x               |             x             |                      |                                         |

[edn1]: https://gitlab.com/domaindrivenarchitecture/c4k-website/-/merge_requests/1
[ing1]:  https://repo.prod.meissa.de/meissa/c4k-jitsi/commit/214aa41c28662fbf7a49998e17404e7ac9216430
[bgrp1]: https://repo.prod.meissa.de/meissa/c4k-jitsi/commit/7ea442adaef727d5b48b242fd0baaaf51902d06e
[mon1]:  https://repo.prod.meissa.de/meissa/c4k-jitsi/commit/19e580188ea56ea26ff3a0bfb08ca428b881ad9a
[val1]:  https://repo.prod.meissa.de/meissa/c4k-jitsi/commit/5f08a108072569473463fb8f19150a12e564e54f
[repo1]: https://repo.prod.meissa.de/meissa/c4k-forgejo/commit/e9ee6136f3347d5fccefa6b5b4a02d30c4dc42e1
[cipyb]: https://gitlab.com/domaindrivenarchitecture/c4k-jitsi/-/merge_requests/1
[macro]: https://repo.prod.meissa.de/meissa/c4k-jitsi/commit/61d05ceedb6dcbc6bb96b96fe6f03598e2878195

## Development & mirrors

Development happens at: https://repo.prod.meissa.de/meissa/c4k-common

Mirrors are:

* https://gitlab.com/domaindrivenarchitecture/c4k-common (issues and PR)
* https://github.com/DomainDrivenArchitecture/c4k-common

For more details about our repository model see: https://repo.prod.meissa.de/meissa/federate-your-repos

## License

Copyright © 2022 meissa GmbH
Licensed under the [Apache License, Version 2.0](LICENSE) (the "License")
Pls. find licenses of our subcomponents [here](doc/SUBCOMPONENT_LICENSE)
