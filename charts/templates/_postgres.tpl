{{- define "rawdata-master.postgresql.fullname" -}}
{{- printf "%s-%s" "postgresql" .Release.Name  | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "rawdata-master.postgresql.labels" -}}
{{ include "rawdata-master.labels" . }}
app.kubernetes.io/component: database
app.kubernetes.io/part-of: rawdata-master
{{- end -}}

{{- define "rawdata-master.postgresql.host" -}}
{{- if .Values.postgresqlOperator.enabled -}}
acid-rawdata-master.{{ .Release.Namespace }}.svc.cluster.local
{{- else if .Values.cloudnativePG.enabled -}}
{{- printf "%s-%s" (include "rawdata-master.postgresql.fullname" .) "rw" | trimSuffix "-" -}}
{{- else -}}
{{- fail "You need to enable .Values.cloudnativePG.enabled or .Values.postgresOperator.enabled" -}}
{{- end -}}
{{- end -}}

{{/*
Set postgres secret
*/}}
{{- define "rawdata-master.postgresql.secret" -}}
{{- if .Values.postgresqlOperator.enabled -}}
rawdatamaster.acid-rawdata-master.credentials.postgresql.acid.zalan.do
{{- else if .Values.cloudnativePG.enabled -}}
{{- printf "%s-%s" (include "rawdata-master.postgresql.fullname" .) "app" | trimSuffix "-" -}}
{{- else -}}
{{- fail "You need to enable .Values.cloudnativePG.enabled or .Values.postgresOperator.enabled" -}}
{{- end -}}
{{- end -}}