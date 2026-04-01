# EKS
output "eks_cluster_name" {
  description = "EKS cluster name — use with: aws eks update-kubeconfig --name <value>"
  value       = aws_eks_cluster.main.name
}

output "eks_cluster_endpoint" {
  description = "EKS API server endpoint"
  value       = aws_eks_cluster.main.endpoint
}

output "eks_cluster_certificate_authority" {
  description = "EKS cluster CA certificate (base64-encoded)"
  value       = aws_eks_cluster.main.certificate_authority[0].data
  sensitive   = true
}

output "eks_oidc_provider_arn" {
  description = "OIDC provider ARN — needed when adding further IRSA roles"
  value       = aws_iam_openid_connect_provider.eks.arn
}

# RDS
output "rds_courierdb_endpoint" {
  description = "RDS courierdb writer endpoint (host:port)"
  value       = "${aws_db_instance.courierdb.address}:${aws_db_instance.courierdb.port}"
}

output "rds_courierdb_replica_endpoint" {
  description = "RDS courierdb read-replica endpoint"
  value       = aws_db_instance.courierdb_replica.address
}

# ElastiCache
output "elasticache_redis_primary_endpoint" {
  description = "ElastiCache Redis primary endpoint"
  value       = aws_elasticache_replication_group.main.primary_endpoint_address
}

output "elasticache_redis_reader_endpoint" {
  description = "ElastiCache Redis reader endpoint"
  value       = aws_elasticache_replication_group.main.reader_endpoint_address
}

# MSK
output "msk_bootstrap_brokers_sasl_iam" {
  description = "MSK bootstrap broker string for SASL/IAM — use as spring.kafka.bootstrap-servers"
  value       = aws_msk_cluster.main.bootstrap_brokers_sasl_iam
}

# S3
output "s3_bucket_delivery_proof_photos" {
  description = "S3 bucket name for delivery proof photos"
  value       = aws_s3_bucket.delivery_proof_photos.bucket
}

output "s3_bucket_delivery_signatures" {
  description = "S3 bucket name for delivery signatures"
  value       = aws_s3_bucket.delivery_signatures.bucket
}

# IAM
output "delivery_service_irsa_role_arn" {
  description = "IAM role ARN for the delivery-service pod (annotate the k8s ServiceAccount with this)"
  value       = aws_iam_role.delivery_service.arn
}
