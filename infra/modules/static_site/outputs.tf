output "bucket_name" {
  value = aws_s3_bucket.dashboard.bucket
}

output "distribution_domain_name" {
  value = aws_cloudfront_distribution.dashboard.domain_name
}

output "distribution_id" {
  value = aws_cloudfront_distribution.dashboard.id
}
