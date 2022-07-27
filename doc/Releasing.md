# stable release (should be done from master)

```
#adjust [version] and remove the -SNAPSHOT
vi project-cljs.clj

git add project-cljs.clj
git commit -m "Prep for release"

lein release
git push --follow-tags

# bump version - increase version and add -SNAPSHOT
vi project-cljs.clj
git commit -am "version bump"
git push
```