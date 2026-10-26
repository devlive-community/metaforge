#!/bin/sh

HOME=$(pwd)
JAVA_HOME=${JAVA_HOME:-/opt/jdk}
APPLICATION_NAME='org.devlive.metaforge.server.MetaForge'
APPLICATION_PID=

check_java_version() {
    local java_version=$("$JAVA_HOME"/bin/java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    local major_version=$(echo "$java_version" | awk -F. '{print $1}')
    if [ "$major_version" != "1" ] && [ "$major_version" != "11" ]; then
        printf "Error: Java version [ %s ] is not supported. Please use Java 1.8 or 11.\n" "$java_version"
        exit 1
    fi
}

job_before_echo_basic() {
    printf "\n\tJob before echo basic \n"
    printf "============================================\n"
    printf "Runtime home                           | %s\n" "$HOME"
    printf "Runtime java home                      | %s\n" "$JAVA_HOME"
    printf "Runtime application name               | %s\n" "$APPLICATION_NAME"
    printf "============================================\n\n"
}

get_jvm_conf() {
    JVM_CONF=`grep -v  "^#" $HOME/configure/jvm.conf | awk '/-/{printf $0" "}' `
}

job_before_apply_server() {
    APPLICATION_PID=$(pgrep -f "$APPLICATION_NAME" | awk '{print $1}')
}

job_runner_checker_server() {
    printf "\n\tJob runner check server \n"
    printf "============================================\n"
    job_before_apply_server
    printf "Runtime process                        | %s\n" "$APPLICATION_PID"
    if test -z "$APPLICATION_PID"; then
        printf "Server status                          | %s\n" "stopped"
        printf "============================================\n\n"
    else
        printf "Server status                          | %s\n" "running"
        printf "============================================\n\n"
        exit
    fi
}

job_runner_start_server() {
    printf "\n\tJob runner server \n"
    printf "============================================\n"
    printf "Server starting                        | %s\n" "$APPLICATION_NAME"
    cd "$HOME"
    get_jvm_conf
    nohup "$JAVA_HOME"/bin/java -cp "$HOME/lib/*" ${JVM_CONF} "$APPLICATION_NAME" \
                  --spring.config.location="$HOME/configure/" > /dev/null 2>&1 &
    sleep 5
    job_before_apply_server
    if test -z "$APPLICATION_PID"; then
        printf "Server start failed                    | %s\n"
    else
        echo "$APPLICATION_PID" >pid
        printf "Server start successful                | %s\n"
    fi
    printf "============================================\n\n"
}

check_java_version
job_before_echo_basic
# shellcheck disable=SC2119
job_runner_checker_server
job_runner_start_server
exit 0
