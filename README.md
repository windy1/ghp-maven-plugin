# GitHub Pages Maven Plugin

The GitHub Pages Maven plugin pushes resources, namely JavaDocs, to a GitHub repository automatically. This is useful
for keeping your GitHub Pages up to date. While this plugin was designed to push API documentation, it can be
re-purposed to automatically push anything to a repository branch.

Walker Crouse is an aspiring software engineer, amateur open source contributor, and starving college student. If you
like my work, please consider donating a small amount so that I can continue to give these projects the time they
deserve. Thank you.

[![Donate](https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=walkercrouse%40hotmail%2ecom&lc=US&item_name=Walker%20Crouse&no_note=0&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donate_LG%2egif%3aNonHostedGuest)


# Quickstart

1. Run

```
mvn archetype:generate \
    -DarchetypeGroupId=se.walkercrou \
    -DarchetypeArtifactId=ghp-maven-archetype \
    -DarchetypeVersion=1.0 \
    -DgroupId=com.example \
    -DartifactId=hello-world
```


2. Develop, Document, Repeat!


3. Run `mvn -P release` and watch your documentation update online!


# Advanced

## Parameters

The following is a table of available parameters for this plugin

| Name | Description | Default | Required |
| ---- | ----------- | ------- | -------- |
| `branch` | The branch to push content in `contentDir` to | `gh-pages` | No |
| `commitMessage` | The commit message to use when updating remote | `...` | No |
| `contentDir` | The directory to copy files from | `apidocs` | No |
| `contentDestination` | The directory to copy files to from the `contentDir` | root | No |

## Command Line

Run this plugin on your project alone with

```
mvn ghp:update \
    -Dbranch=my-branch \
    -DcommitMessage="Custom commit message" \
    -DcontentDir=my-content \
    -DcontentDestination=docs \
```

## Configuration

The following is a standard configuration for this plugin

```xml
<configuration>
    <branch>my-branch</branch>
    <commitMessage>Custom commit message</commitMessage>
    <contentDir>my-content</contentDir>
    <contentDestination>docs</contentDestination>
</configuration>
```