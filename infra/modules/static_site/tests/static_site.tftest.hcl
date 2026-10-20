mock_provider "aws" {
  override_data {
    target = data.aws_iam_policy_document.dashboard_bucket_policy
    values = {
      json = jsonencode({
        Version = "2012-10-17"
        Statement = [{
          Sid       = "AllowCloudFrontRead"
          Effect    = "Allow"
          Action    = "s3:GetObject"
          Principal = { Service = "cloudfront.amazonaws.com" }
        }]
      })
    }
  }
}
mock_provider "random" {}

variables {
  project_name   = "bootupgrade"
  environment    = "dev"
  domain_aliases = []
}

run "bucket_is_private_and_versioned" {
  command = plan

  assert {
    condition     = aws_s3_bucket_public_access_block.dashboard.block_public_acls == true
    error_message = "The dashboard bucket must block public ACLs — it's served only via CloudFront"
  }

  assert {
    condition     = aws_s3_bucket_public_access_block.dashboard.restrict_public_buckets == true
    error_message = "The dashboard bucket must restrict public bucket policies"
  }

  assert {
    condition     = aws_s3_bucket_versioning.dashboard.versioning_configuration[0].status == "Enabled"
    error_message = "Versioning must be enabled on the dashboard bucket"
  }
}

run "cloudfront_serves_https_only_with_spa_fallback" {
  command = plan

  assert {
    condition     = aws_cloudfront_distribution.dashboard.default_cache_behavior[0].viewer_protocol_policy == "redirect-to-https"
    error_message = "CloudFront must redirect HTTP viewers to HTTPS"
  }

  assert {
    condition = alltrue([
      for resp in aws_cloudfront_distribution.dashboard.custom_error_response :
      resp.response_code == 200 && resp.response_page_path == "/index.html"
    ])
    error_message = "403/404 from S3 must fall back to index.html so Angular's client-side router can handle deep links"
  }
}

run "bucket_policy_scoped_to_this_distribution_only" {
  command = apply

  assert {
    condition     = strcontains(data.aws_iam_policy_document.dashboard_bucket_policy.json, "cloudfront.amazonaws.com")
    error_message = "Only the CloudFront service principal may read from the bucket"
  }
}
