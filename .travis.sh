#!/bin/bash

set -e

export DOCKER_HOST=tcp://127.0.0.1:2375

if [ ${TRAVIS_PULL_REQUEST} == "false" ]; then
    if [[ ${TRAVIS_BRANCH} =~ develop|master|release ]]; then
    	if [[ ${TRAVIS_BRANCH} =~ develop|master ]]; then
    		export SONAR_BRANCH=${TRAVIS_BRANCH}
    	else
    		export SONAR_BRANCH="release"
    	fi
        mvn -B -Psonar verify
        mvn -B sonar:sonar -Dsonar.host.url=-Dsonar.host.url=https://sonarqube.kaazing.us -Dsonar.login=${SONAR_USER} -Dsonar.password=${SONAR_PASSWORD} -Dsonar.branch=${SONAR_BRANCH}
    else
        mvn -B verify
        echo "SonarQube analysis is performed only for commits on the develop, master and release/* branches (not for PRs)"
    fi
else
    mvn -B -U verify
    echo "SonarQube analysis is performed only for commits on the develop, master and release/* branches (not for PRs)"
fi
