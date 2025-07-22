# Rawdata-Master Helm CHart

This Chart deploys the Rawdata-Master in Kubernetes

## Development

## Create Namespace
In the EU, we've to create the Namespace with the timref-custom chart.
```shell
export NameSpace='rawdata-master'
```

```shell
kubectl create namespace rawdata-master
```

## Install the HELM-Chart
```shell
    helm upgrade --install rawdata-master . --namespace rawdata-master --create-namespace -f values.yaml -f development/$ENV
```
