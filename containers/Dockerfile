ARG BASE_IMAGE=ndviet/test-automation-java-common:latest
FROM ${BASE_IMAGE}

ARG GITHUB_ACTOR=""
ARG GITHUB_TOKEN=""

WORKDIR /workspace

COPY gradle/ gradle/
COPY gradlew gradlew.bat build.gradle settings.gradle gradle.properties ./
RUN chmod +x gradlew

RUN ./gradlew dependencies --no-daemon \
      -Pgpr.user="${GITHUB_ACTOR}" \
      -Pgpr.token="${GITHUB_TOKEN}" \
      --configuration testRuntimeClasspath

RUN GRADLE_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}" \
    && mkdir -p "${GRADLE_HOME}/init.d" \
    && cat > "${GRADLE_HOME}/init.d/freeze-snapshot-cache.gradle" <<'EOF'
allprojects {
    configurations.all {
        resolutionStrategy {
            cacheChangingModulesFor 999999, 'hours'
            cacheDynamicVersionsFor 999999, 'hours'
        }
    }
}
EOF

COPY src/ src/

RUN ./gradlew testClasses --no-daemon \
      -Pgpr.user="${GITHUB_ACTOR}" \
      -Pgpr.token="${GITHUB_TOKEN}"

CMD ["./gradlew", "test", "--no-daemon", "--info"]
