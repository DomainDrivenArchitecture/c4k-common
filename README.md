# convention 4 kubernetes: c4k-common
[![Clojars Project](https://img.shields.io/clojars/v/org.domaindrivenarchitecture/c4k-common-clj.svg)](https://clojars.org/org.domaindrivenarchitecture/c4k-common-clj) [![Clojars Project](https://img.shields.io/clojars/v/org.domaindrivenarchitecture/c4k-common-cljs.svg)](https://clojars.org/org.domaindrivenarchitecture/c4k-common-cljs) [![pipeline status](https://gitlab.com/domaindrivenarchitecture/c4k-common/badges/master/pipeline.svg)](https://gitlab.com/domaindrivenarchitecture/c4k-common/-/commits/master) 

[<img src="https://domaindrivenarchitecture.org/img/delta-chat.svg" width=20 alt="DeltaChat"> chat over e-mail](mailto:buero@meissa-gmbh.de?subject=community-chat) | [<img src="https://meissa-gmbh.de/img/community/Mastodon_Logotype.svg" width=20 alt="team@social.meissa-gmbh.de"> team@social.meissa-gmbh.de](https://social.meissa-gmbh.de/@team) | [Website & Blog](https://domaindrivenarchitecture.org)

## Purpose

c4k-common ....

## Rational

There are many comparable solutions for creating c4k deployments like helm or kustomize. Why do we need another one?
* We like the simplicity of kustomize. Yaml in, yaml out, the ability to lint the result and the option to split large yaml files into objects. But a simple overwriting per environment may not be enough ...
* We like helm packages. A package encapsulates the setup for an application. On the one hand, but on the other hand we don't like the idea of having to program and debug in a template language. We can program much better in real programming languages.

Our convention 4 kubernetes c4k-* tools combine the advantages of both approaches:
* Packages for one application
* Programming in clojure
* yaml / edn as input and output, no more magic
* good validation, integration as api, cli or in the browser

## Usage


## Refactoring & Module Overview

| Module           | Version | common postgres | frontend script | provider adapter |
|------------------|---------|:---------------:|:---------------:|:----------------:|
| c4k-mastodon-bot | 0.1     |       -         |                 |                  |
| c4k-keycloak     | 0.2     |                 |                 |                  |
| c4k-jira         | 1.0     |       x         |       x         |                  |
| c4k-nextcloud    | 0.1     |                 |                 |                  |

## License

Copyright © 2021 meissa GmbH
Licensed under the [Apache License, Version 2.0](LICENSE) (the "License")
Pls. find licenses of our subcomponents [here](doc/SUBCOMPONENT_LICENSE)