{{/*
Expand the name of the chart.
*/}}
{{- define "deli-common.name" -}}
{{- .Chart.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Full name: release-name + chart-name, truncated to 63 chars.
*/}}
{{- define "deli-common.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels applied to all resources.
*/}}
{{- define "deli-common.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: deli-platform
{{- end }}

{{/*
Selector labels — used in matchLabels and pod template labels.
Must be stable across upgrades (do not include chart version).
*/}}
{{- define "deli-common.selectorLabels" -}}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
ClusterIP Service template.
*/}}
{{- define "deli-common.service" -}}
apiVersion: v1
kind: Service
metadata:
  name: {{ include "deli-common.fullname" . }}
  namespace: {{ .Release.Namespace }}
  labels:
    {{- include "deli-common.labels" . | nindent 4 }}
spec:
  type: ClusterIP
  selector:
    {{- include "deli-common.selectorLabels" . | nindent 4 }}
  ports:
    - name: http
      port: {{ .Values.service.port | default 8080 }}
      targetPort: http
      protocol: TCP
    {{- if .Values.service.wsPort }}
    - name: ws
      port: {{ .Values.service.wsPort }}
      targetPort: ws
      protocol: TCP
    {{- end }}
{{- end }}

{{/*
ServiceAccount name.
*/}}
{{- define "deli-common.serviceAccountName" -}}
delivery-service-account
{{- end }}
