#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'net/http'
require 'securerandom'
require 'time'
require 'uri'

endpoint = URI(ENV.fetch('AOO_LOCAL_ACCOUNT_URL', 'http://127.0.0.1:8096'))
abort 'refusing non-loopback account endpoint' unless %w[127.0.0.1 localhost ::1].include?(endpoint.host)
abort 'set AOO_LOCAL_TEST_ACCOUNT_CONFIRM=aoo_login_local' unless ENV['AOO_LOCAL_TEST_ACCOUNT_CONFIRM'] == 'aoo_login_local'

headers = {
  'X-Aoo-Api-Version' => '1',
  'X-Client-Channel' => 'stable',
  'X-Client-Version' => '0.1.0',
  'Content-Type' => 'application/x-www-form-urlencoded;charset=UTF-8'
}.freeze

def post(endpoint, path, form, headers, device)
  uri = endpoint + path
  request = Net::HTTP::Post.new(uri)
  headers.each { |key, value| request[key] = value }
  request['X-Device-Id'] = device
  request.set_form_data(form)
  response = Net::HTTP.start(uri.host, uri.port, open_timeout: 3, read_timeout: 10) { |http| http.request(request) }
  [response.code.to_i, JSON.parse(response.body)]
end

created = []
existing = []
(21..100).each do |number|
  login = number.to_s
  device = "local-test-account-init-#{login}"
  status, issued = post(endpoint, '/api/v2/account/register/code', { identity: login }, headers, device)
  if status == 409 && issued['code'] == 'ACCOUNT_ALREADY_EXISTS'
    existing << login
    next
  end
  abort "verification code request failed for #{login}: HTTP #{status} #{issued}" unless status == 200
  code = issued.fetch('verificationCode')
  status, account = post(endpoint, '/api/v2/account/register', {
    login: login,
    password: '1',
    recovery: "local-test-account-#{login}-#{SecureRandom.uuid}",
    verificationCode: code
  }, headers, device)
  abort "registration failed for #{login}: HTTP #{status} #{account}" unless status == 200
  created << { username: login, accountId: account.fetch('accountId'), displayId: account.fetch('displayId') }
end

verified = []
[21, 30].each do |number|
  login = number.to_s
  device = "login-lobby-ai-validation-#{login}"
  status, tokens = post(endpoint, '/api/v2/account/login', { identity: login, password: '1', mode: 'multi' }, headers, device)
  abort "login failed for assigned account #{login}: HTTP #{status} #{tokens}" unless status == 200 && tokens['accessToken']
  uri = endpoint + '/api/v2/account/session'
  request = Net::HTTP::Get.new(uri)
  headers.reject { |key, _| key == 'Content-Type' }.each { |key, value| request[key] = value }
  request['X-Device-Id'] = device
  request['Authorization'] = "Bearer #{tokens.fetch('accessToken')}"
  response = Net::HTTP.start(uri.host, uri.port, open_timeout: 3, read_timeout: 10) { |http| http.request(request) }
  principal = JSON.parse(response.body)
  abort "session verification failed for #{login}: HTTP #{response.code} #{principal}" unless response.code.to_i == 200
  verified << { username: login, accountId: principal.fetch('accountId'), displayId: principal.fetch('displayId') }
end

puts JSON.pretty_generate({
  environment: 'local', database: 'aoo_login_local', endpoint: endpoint.to_s,
  created: created, alreadyExisting: existing, verifiedByLoginLobby: verified,
  reservedRange: '51-100', generatedAt: Time.now.utc.iso8601
})
