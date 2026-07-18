#!/bin/bash
# Gradle wrapper script for MO MACRO
# This file will be executed in GitHub Actions Ubuntu environment

# Determine the project root directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Download Gradle if needed and execute
if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "Gradle wrapper not found. Please ensure gradle-wrapper.jar is present."
    exit 1
fi

# Execute Gradle with all passed arguments
exec java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain "$@"
