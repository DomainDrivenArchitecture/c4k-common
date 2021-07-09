# stable release (should be done from master)

```
#adjust [version]
vi project.clj
vi project-cljs.clj

git commit -am "release"
git tag -am "release" [release version no]
git push --follow-tags

# bump version - increase version and add -SNAPSHOT
vi project.clj
vi project-cljs.clj
git commit -am "version bump"
git push
```