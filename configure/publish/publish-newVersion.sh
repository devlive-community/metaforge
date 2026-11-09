#!/bin/sh
VERSION=$1
HOME=$(pwd)
CURRENT_DATE=$(date '+%Y-%m-%d %H:%M:%S')

job_before_echo_basic() {
    printf "\n\tJob before echo basic \n"
    printf "============================================\n"
    printf "Released new version\n"
    printf "============================================\n\n"
}

job_before_checked() {
    printf "\n\tJob before check parameter \n"
    printf "============================================\n"
    if test -z "$VERSION"; then
        printf "Please input version \n"
        exit 1
    else
        printf "Version checked successful         | %s\n" "$VERSION"
    fi
    printf "============================================\n\n"
}

job_runner_apply() {
    printf "\n\tJob runner apply \n"
    printf "============================================\n"
    printf "Apply new version for server ...\n"
    MAVEN_OPTS=-Dorg.slf4j.simpleLogger.defaultLogLevel=error ./mvnw versions:set -DnewVersion="$VERSION"
    if [ $? -ne 0 ]; then
        printf "\nApply new version for server failed\n\n"
        exit 1
    else
        printf "\nApply new version for server successful\n\n"
    fi

    echo "Apply new version for application ..."
    perl -pi -e 's/app\.version=.*/app.version='"$VERSION"'/g' configure/etc/conf/application.properties

    echo "Apply new version for plugin ..."
    perl -pi -e 's/VERSION=.*/VERSION='"$VERSION"'/g' configure/etc/bin/install-plugin.sh
    perl -pi -e 's/VERSION=.*/VERSION='"$VERSION"'/g' configure/etc/bin/install-plugin.bat

    echo "Apply new version for metadata ..."
    # Update version and URL in metadata.json using perl
    # 更新 version 字段
    perl -i -pe 's/"version": "[^"]*"/"version": "'"$VERSION"'"/' "$HOME/configure/metadata.json"

    # 更新 url 字段中的版本号 (针对类似 2024.4.0 这样的版本格式)
    # Update the version number in the url field (for a version format like 2024.4.0)
    perl -i -pe 's/\/\d{4}\.\d+\.\d+\//\/'"$VERSION"'\//' "$HOME/configure/metadata.json"
    perl -i -pe 's/-\d{4}\.\d+\.\d+-bin/-'"$VERSION"'-bin/' "$HOME/configure/metadata.json"

    # 更新发布日期
    # Update publish date
    perl -i -pe 's/"released": "[^"]*"/"released": "'"$CURRENT_DATE"'"/' "$HOME/configure/metadata.json"

    printf "Apply new version for web ...\n"
    # shellcheck disable=SC2164
    cd "$HOME"/core/metaforge-ui
    npm version "$VERSION" --no-git-tag-version
    if [ $? -ne 0 ]; then
        printf "\nApply new version for web failed\n\n"
        exit 1
    else
        printf "\nApply new version for web successful\n\n"
    fi
    cd "$HOME"
    printf "============================================\n\n"
}

job_before_echo_basic
job_before_checked
job_runner_apply