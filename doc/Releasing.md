# stable release (should be done from master)

```
#adjust [version]
vi project-cljs.clj

lein release
git push --follow-tags

# bump version - increase version and add -SNAPSHOT
vi project-cljs.clj
git commit -am "version bump"
git push
```