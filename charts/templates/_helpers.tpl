{{/*
Expand the name of the chart.
*/}}
{{- define "rawdata-master.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "rawdata-master.upload-mock.name" -}}
{{- printf "%s-%s" ( include "rawdata-master.name" . ) "upload-mock" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "rawdata-master.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create a default fully qualified app name for upload-mock.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "rawdata-master.upload-mock.fullname" -}}
{{- printf "%s-%s" ( include "rawdata-master.fullname" . ) "upload-mock" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "rawdata-master.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "rawdata-master.labels" -}}
helm.sh/chart: {{ include "rawdata-master.chart" . }}
{{ include "rawdata-master.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/component: "rawdata-master"
app.kubernetes.io/part-of: {{ .Chart.Name }}
{{- end }}

{{/*
Common labels upload-mock
*/}}
{{- define "rawdata-master.upload-mock.labels" -}}
helm.sh/chart: {{ include "rawdata-master.chart" . }}
{{ include "rawdata-master.upload-mock.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/component: "rawdata-master"
app.kubernetes.io/part-of: {{ .Chart.Name }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "rawdata-master.selectorLabels" -}}
app.kubernetes.io/name: {{ include "rawdata-master.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Selector labels upload-mock
*/}}
{{- define "rawdata-master.upload-mock.selectorLabels" -}}
app.kubernetes.io/name: {{ include "rawdata-master.upload-mock.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "rawdata-master.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "rawdata-master.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}