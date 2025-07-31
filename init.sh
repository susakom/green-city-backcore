#!/bin/bash

psql -U greencity -d greencity -f /docker-entrypoint-initdb.d/03_data.sql
