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
2. common postgres
3. frontend script
4. backup as deployment
5. use common pred.
6. configs as EDN and YAML
7. renamed test-helper
8. common load-as-edn
9. standardized uberjar
10. standardized resources
11. groups for webview
12. use common ingress
-->

| Module           | Version | common postgres  | frontend script | [backup as deployment][bak1] | [use common pred. ][com1] | [renamed test-helper][th1] | [common load-as-edn][edn1] | [standardized uberjar][ujar] | groups for webview | [use common ingress][ing1] | use common monitoring |
|------------------|---------|:----------------:|:---------------:|:----------------------------:|:-------------------------:|:--------------------------:|:--------------------------:|:----------------------------:|:------------------:|:--------------------------:|:---------------------:|
| c4k-mastodon-bot | 0.0     |        -         |                 |              -               |                           |                            |                            |                              |                    |                            |                       |
| c4k-keycloak     | 0.2     |                  |                 |                              |                           |                            |                            |                              |                    |                            |                       | 
| c4k-jira         | 1.1     |        x         |        x        |              x               |             x             |             -              |                            |                              |                    |                            |                       |
| c4k-nextcloud    | 4.0     |        x         |        x        |              x               |             x             |             -              |                            |                              |                    |                            |                       |
| c4k-jitsi        | 1.2     |        -         |        x        |              -               |             x             |             -              |                            |              x               |                    |                            |                       |
| c4k-gittea       | 1.0     |        x         |        x        |              x               |             x             |             x              |                            |              x               |         x          |                            |                       |
| c4k-shynet       | 1.0     |        x         |        x        |              -               |             x             |             -              |                            |              x               |                    |                            |                       |
| c4k-website      | 0.2     |        x         |        x        |              x               |             x             |             x              |             x              |              x               |         x          |             x              |                       |

[bak1]: https://gitlab.com/domaindrivenarchitecture/c4k-jira/-/merge_requests/1
[com1]: https://gitlab.com/domaindrivenarchitecture/c4k-nextcloud/-/merge_requests/3
[yaml1]: https://gitlab.com/domaindrivenarchitecture/c4k-nextcloud/-/merge_requests/4
[th1]: https://gitlab.com/domaindrivenarchitecture/c4k-gitea/-/merge_requests/1
[edn1]: https://gitlab.com/domaindrivenarchitecture/c4k-website/-/merge_requests/1
[ing1]: https://gitlab.com/domaindrivenarchitecture/c4k-website/-/merge_requests/2
[ujar]: https://gitlab.com/domaindrivenarchitecture/c4k-jitsi/-/commit/b852a74dc561c3ab619e4f4d0748ab51e75edc13

## License

Copyright © 2022 meissa GmbH
Licensed under the [Apache License, Version 2.0](LICENSE) (the "License")
Pls. find licenses of our subcomponents [here](doc/SUBCOMPONENT_LICENSE)