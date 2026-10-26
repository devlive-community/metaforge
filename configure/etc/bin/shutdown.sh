#!/bin/sh

HOME=$(pwd)
APPLICATION_NAME='org.devlive.metaforge.server.MetaForge'
APPLICATION_PID=

job_before_echo_basic() {
    printf "\n\tJob before echo basic \n"
    printf "============================================\n"
    printf "Runtime home                           | %s\n" "$HOME"
    printf "Runtime application name               | %s\n" "$APPLICATION_NAME"
    printf "============================================\n\n"
}

job_before_apply_server() {
    APPLICATION_PID=$(pgrep -f "$APPLICATION_NAME" | awk '{print $1}')
}

job_runner_stop_server() {
    printf "\n\tJob runner check server \n"
    printf "============================================\n"
    job_before_apply_server
    printf "Runtime process                        | %s\n" "$APPLICATION_PID"
    if test -z "$APPLICATION_PID"; then
        printf "Server status                          | %s\n" "stopped"
        printf "============================================\n\n"
        exit
    else
        printf "Server stopping                        | %s\n" "$APPLICATION_NAME"
        kill -9 "$APPLICATION_PID"
        rm -rf "$HOME/pid"
        printf "Server stopped successfully            | %s\n"
        printf "============================================\n\n"
    fi
}

job_before_echo_basic
job_runner_stop_server
