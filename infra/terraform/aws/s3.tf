locals {
  s3_buckets = {
    delivery_proof_photos = "${var.cluster_name}-${var.environment}-delivery-proof-photos"
    delivery_signatures   = "${var.cluster_name}-${var.environment}-delivery-signatures"
  }
}

resource "aws_s3_bucket" "delivery_proof_photos" {
  bucket = local.s3_buckets.delivery_proof_photos

  tags = {
    Name = local.s3_buckets.delivery_proof_photos
  }
}

resource "aws_s3_bucket" "delivery_signatures" {
  bucket = local.s3_buckets.delivery_signatures

  tags = {
    Name = local.s3_buckets.delivery_signatures
  }
}

resource "aws_s3_bucket_versioning" "delivery_proof_photos" {
  bucket = aws_s3_bucket.delivery_proof_photos.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_versioning" "delivery_signatures" {
  bucket = aws_s3_bucket.delivery_signatures.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "delivery_proof_photos" {
  bucket = aws_s3_bucket.delivery_proof_photos.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "delivery_signatures" {
  bucket = aws_s3_bucket.delivery_signatures.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "delivery_proof_photos" {
  bucket                  = aws_s3_bucket.delivery_proof_photos.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_public_access_block" "delivery_signatures" {
  bucket                  = aws_s3_bucket.delivery_signatures.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
