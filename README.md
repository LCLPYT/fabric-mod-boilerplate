# fabric-mod-boilerplate
A boilerplate to quickly setup a fabric mod.

## Needed configuration
- `gradle.properties`:
Change `maven-group` and `archives_base_name`; configure Fabric versions if needed
- rename base package name in `src/main/java/org/example/modid`, insert your custom modid
- `src/main/resources/fabric.mod.json`: change `modid`, `name`, `description`, `authors`, `entrypoints/{main,client}`, `mixins` (adjust `modid.mixins.json`)
- rename `src/main/resources/modid.mixins.json`
- rename classes `org.example.ExampleMod` and `org.example.client.ExampleModClient`

## Versioning
This boilerplate uses the [`gradle-build-utils`](https://github.com/LCLPYT/GradleBuildUtils) Gradle plugin, which will determine the current version from the latest Git tag.
To update the version, commit your changes and tag it with a semantic version comform tag.

Example:
```
git tag 1.0.0
```

## Publishing
If you want to publish your mod from CLI, you can create a `publish.properties` file in the project root.
The contents should look like this:
```properties
mavenHost=https://your.maven.repo.here
mavenUser=user
mavenPassword=password
```
When the file is not present, or doesn't contain these entries, the filesystem will be used as fallback (in the `repo/` directory).

### GitHub Actions
If you are using GitHub Actions to publish your mod, you can define Actions secrets to authenticate.
Just define `DEPLOY_URL`, `DEPLOY_USER` and `DEPLOY_PASSWORD` as Action secrets on your repository.
Don't forget to pass them as environment variables your the action definition.

The `gradle-build-utils` Gradle plugin will not always succeed at getting your Git tag version.
As reliable workaround, it is recommended to set a `CI_VERSION` environment variable that can be determined quite easily by GitHub Actions.

As an example GitHub Action definition, you can use [the publish action from MMOContent](https://github.com/LCLPYT/MMOContent/blob/c89ca987f2f451b524313c06401e8e4a2b5d6de5/.github/workflows/gradle-publish.yml).
