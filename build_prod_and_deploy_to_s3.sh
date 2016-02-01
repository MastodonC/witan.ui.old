#!/usr/bin/env bash

# build
lein clean
WITAN_API_URL=http://witan-alpha.mastodonc.com:80 LOG_LEVEL=2 lein cljsbuild once prod
lein garden once

# cp
mkdir target/build
cp -r resources/public/* target/build/

# aws
aws s3 sync --profile witan target/build/. s3://witan-ui

#
echo "Finished:"
echo "http://witan-ui.s3-website.eu-central-1.amazonaws.com/"
