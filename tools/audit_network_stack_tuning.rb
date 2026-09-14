#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
output = File.join(root, 'work/audit/network-stack-tuning.json')
patterns = {
  netty: /(?:io\.netty|netty-)/,
  mina: /(?:org\.apache\.mina|mina-core)/,
  eventLoop: /(?:EventLoopGroup|NioEventLoopGroup|EpollEventLoopGroup|KQueueEventLoopGroup)/,
  pooledAllocator: /(?:PooledByteBufAllocator|ALLOCATOR)/,
  compression: /(?:WebSocketServerCompressionHandler|Zlib|CompressionHandler|permessage-deflate)/,
  tls: /(?:SslContext|SslHandler|TLSv1\.[23]|wss)/i,
  writeWatermark: /(?:WRITE_BUFFER_WATER_MARK|WriteBufferWaterMark)/,
  idleTimeout: /(?:IdleStateHandler|readTimeout|writeTimeout)/
}.freeze
findings = patterns.keys.to_h { |key| [key, []] }
Dir.glob(File.join(root, 'server/**/*')).each do |path|
  next unless File.file?(path) && %w[.java .kt .xml .properties .yml .yaml].include?(File.extname(path))
  next if path.include?('/target/') || path.include?('/build/')
  text = File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace)
             .encode('UTF-8', invalid: :replace, undef: :replace, replace: '')
  patterns.each { |key, pattern| findings[key] << path.delete_prefix(root + '/') if text.match?(pattern) }
end
counts = findings.transform_values { |paths| paths.uniq.size }
single_stack = counts[:netty].positive? && counts[:mina].zero?
required_tuning = %i[eventLoop pooledAllocator compression tls writeWatermark idleTimeout].all? { |key| counts[key].positive? }
summary = counts.merge(singleProductionStack: single_stack, completeStaticTuning: required_tuning,
                       longConnectionLoadEvidencePresent: false, passed: single_stack && required_tuning && false)
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'One production network stack has measured EventLoop, allocator, compression, TLS, backpressure and idle-timeout settings under long connections.',
          summary: summary, evidence: findings.transform_values(&:uniq),
          requiredRuntimeEvidence: %w[connection-count handshake-latency event-loop-lag direct-memory allocation-rate compression-ratio backpressure tls-errors]}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_NETWORK_TUNING_GATE'] == '1'
