#!/usr/bin/env ruby
# frozen_string_literal: true

require 'fileutils'
require 'json'
require 'net/http'
require 'open3'
require 'securerandom'
require 'time'
require 'uri'

ROOT = File.expand_path('../../..', __dir__)
OUT = File.join(ROOT, 'work/fw035/evidence')
FileUtils.mkdir_p(OUT)

def run(command, chdir: ROOT)
  stdout, stderr, status = Open3.capture3(command, chdir: chdir)
  [stdout, stderr, status.exitstatus]
end

def command_record(command, chdir: ROOT)
  stdout, stderr, code = run(command, chdir: chdir)
  "$ #{command}\n--- stdout ---\n#{stdout}--- stderr ---\n#{stderr}--- exit_code ---\n#{code}\n"
end

def line_count(path)
  return 0 unless File.file?(path)
  File.foreach(path).count
end

def appended_lines(path, before)
  return '' unless File.file?(path)
  File.readlines(path, chomp: false).drop(before).join
end

def request(method, url, headers: {}, body: nil, form: nil)
  uri = URI(url)
  http = Net::HTTP.new(uri.host, uri.port)
  http.open_timeout = 10
  http.read_timeout = 30
  klass = { get: Net::HTTP::Get, post: Net::HTTP::Post }.fetch(method)
  req = klass.new(uri)
  headers.each { |key, value| req[key] = value }
  if form
    req.set_form_data(form)
  elsif !body.nil?
    req.body = JSON.generate(body)
  end
  response = http.request(req)
  [response.code.to_i, response.each_header.to_h, response.body.to_s]
rescue StandardError => e
  [0, {}, "#{e.class}: #{e.message}"]
end

def parse_json(text)
  JSON.parse(text)
rescue JSON::ParserError
  nil
end

def redact_login_tokens(text)
  parsed = parse_json(text)
  return text.gsub(/("(?:accessToken|refreshToken)"\s*:\s*")[^"]*(")/i, '\\1[REDACTED]\\2') unless parsed

  redact = lambda do |value|
    case value
    when Hash
      value.each_with_object({}) do |(key, child), result|
        result[key] = %w[accesstoken refreshtoken].include?(key.to_s.downcase) ? '[REDACTED]' : redact.call(child)
      end
    when Array
      value.map { |child| redact.call(child) }
    else
      value
    end
  end
  JSON.pretty_generate(redact.call(parsed))
end

def deep_find(object, key)
  case object
  when Hash
    return object[key] if object.key?(key)
    object.each_value do |value|
      found = deep_find(value, key)
      return found unless found.nil?
    end
  when Array
    object.each do |value|
      found = deep_find(value, key)
      return found unless found.nil?
    end
  end
  nil
end

def record_http(io, label, method, url, headers, request_body, code, response_headers, response_body)
  recorded_headers = headers.dup
  recorded_headers['Authorization'] = 'Bearer [REDACTED]' if recorded_headers.key?('Authorization')
  io << "\n===== #{label} =====\n"
  io << "method: #{method.to_s.upcase}\nurl: #{url}\n"
  io << "request_headers:\n#{JSON.pretty_generate(recorded_headers)}\n"
  io << "request_body:\n#{request_body.is_a?(String) ? request_body : JSON.pretty_generate(request_body)}\n"
  io << "http_code: #{code}\nresponse_headers:\n#{JSON.pretty_generate(response_headers)}\n"
  io << "response_body:\n#{response_body}\n"
end

runtime = []
runtime << "$ date\n#{Time.now}\n--- exit_code ---\n0\n"
runtime << command_record(%q{ps -eo pid,ppid,lstart,etime,command | grep -E 'gateway|hall|local-runtime|java' | grep -v grep})
runtime << command_record(%q{lsof -nP -iTCP -sTCP:LISTEN | grep -E ':(3306|8080|8093|8095|8096|8097|8101|8102|8765|18080|7461)\b'})
runtime << command_record('tools/local-dev-services.sh status')
File.write(File.join(OUT, '00-runtime-facts.txt'), runtime.join("\n"))

lobby = []
lobby << command_record(%q{curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:7461/})
lobby << command_record('curl -s -D - -o /dev/null http://127.0.0.1:7461/')
File.write(File.join(OUT, '01-lobby-reachable.txt'), lobby.join("\n"))
lobby_code = lobby.first[/--- stdout ---\n(\d+)/, 1] || 'unknown'

logs = {
  'hall.log' => File.join(ROOT, 'logs/local-dev/hall.log'),
  'gateway.log' => File.join(ROOT, 'logs/local-dev/gateway.log')
}
before = logs.transform_values { |path| line_count(path) }

api = +"FW-035 hall availability check at #{Time.now.iso8601}\n"
login_base = 'http://127.0.0.1:8096'
gateway = 'http://127.0.0.1:8080'
origin = 'http://127.0.0.1:7461'
tokens = {}

%w[21 22].each do |account|
  url = "#{login_base}/api/v2/account/login"
  headers = {
    'Content-Type' => 'application/x-www-form-urlencoded',
    'X-Aoo-Api-Version' => '1', 'X-Client-Channel' => 'stable',
    'X-Client-Version' => '0.1.0', 'X-Device-Id' => "fw035-diag-#{account}",
    'X-Origin' => origin
  }
  form = { 'identity' => account, 'password' => '1', 'mode' => 'multi' }
  code, response_headers, response_body = request(:post, url, headers: headers, form: form)
  record_http(api, "login account #{account}", :post, url, headers, URI.encode_www_form(form), code, response_headers, redact_login_tokens(response_body))
  tokens[account] = deep_find(parse_json(response_body), 'accessToken')
end

def auth_headers(token, origin, trace)
  {
    'Authorization' => "Bearer #{token}", 'Origin' => origin,
    'X-Aoo-Api-Version' => '1', 'X-Client-Version' => '3.8.8',
    'Content-Type' => 'application/json', 'X-Trace-Id' => "fw035-#{trace}"
  }
end

catalog_url = "#{gateway}/api/v2/hall/catalog?clientVersion=3.8.8"
headers = auth_headers(tokens['21'], origin, 'catalog')
catalog_code, rh, rb = request(:get, catalog_url, headers: headers)
record_http(api, 'catalog', :get, catalog_url, headers, '', catalog_code, rh, rb)
catalog = parse_json(rb)

filters_url = "#{gateway}/api/v2/hall/play-filters?clientVersion=3.8.8"
headers = auth_headers(tokens['21'], origin, 'play-filters')
filters_code, rh, rb = request(:get, filters_url, headers: headers)
record_http(api, 'play-filters', :get, filters_url, headers, '', filters_code, rh, rb)

games = {
  'CD201' => { game_id: 8, play_version: '1.0.0' },
  'LS201' => { game_id: 90_005, play_version: 'xqp-equivalent-1' },
  'MN201' => { game_id: 631, play_version: 'mn201-v1.0.0' }
}
results = {}

games.each do |game_code, facts|
  query = URI.encode_www_form(gameCode: game_code, gameId: facts[:game_id], playVersion: facts[:play_version], clientVersion: '3.8.8')
  config_url = "#{gateway}/api/v2/hall/configuration?#{query}"
  headers = auth_headers(tokens['21'], origin, "#{game_code.downcase}-configuration")
  config_code, rh, config_body = request(:get, config_url, headers: headers)
  record_http(api, "#{game_code} configuration", :get, config_url, headers, '', config_code, rh, config_body)
  config = parse_json(config_body)
  fields = deep_find(config, 'fields')
  rules = Array(fields).each_with_object({}) do |field, memo|
    next unless field.is_a?(Hash) && field.key?('key') && field['disabled'] != true
    memo[field['key']] = field['defaultValue']
  end
  create_request_id = SecureRandom.uuid
  create_body = {
    requestId: create_request_id, gameId: facts[:game_id], playVersion: facts[:play_version],
    clientVersion: '3.8.8', rules: rules, scope: { type: 'PERSONAL' }
  }
  create_url = "#{gateway}/api/v2/hall/rooms"
  headers = auth_headers(tokens['21'], origin, "#{game_code.downcase}-create")
  headers['Idempotency-Key'] = create_request_id
  create_code, rh, create_response = request(:post, create_url, headers: headers, body: create_body)
  record_http(api, "#{game_code} create (actual rules included)", :post, create_url, headers, create_body, create_code, rh, create_response)
  parsed_create = parse_json(create_response)
  room_id = deep_find(parsed_create, 'roomId')
  results[game_code] = {
    create_code: create_code, room_id: room_id,
    response_code: deep_find(parsed_create, 'code'), response_message: deep_find(parsed_create, 'message')
  }
  next unless %w[CD201 LS201].include?(game_code) && room_id

  join_url = "#{gateway}/api/v2/hall/rooms/#{room_id}/join"
  join_headers = auth_headers(tokens['22'], origin, "#{game_code.downcase}-join")
  join_headers['Idempotency-Key'] = SecureRandom.uuid
  join_code, rh, join_response = request(:post, join_url, headers: join_headers, body: {})
  record_http(api, "#{game_code} join account 22", :post, join_url, join_headers, {}, join_code, rh, join_response)
  results[game_code][:join_code] = join_code

  room_url = "#{gateway}/api/v2/hall/rooms/#{room_id}"
  get_headers = auth_headers(tokens['21'], origin, "#{game_code.downcase}-room")
  room_code, rh, room_response = request(:get, room_url, headers: get_headers)
  record_http(api, "#{game_code} room detail", :get, room_url, get_headers, '', room_code, rh, room_response)
  results[game_code][:room_get_code] = room_code

  %w[22 21].each do |account|
    leave_url = "#{gateway}/api/v2/hall/rooms/#{room_id}/leave"
    leave_headers = auth_headers(tokens[account], origin, "#{game_code.downcase}-leave-#{account}")
    leave_headers['Idempotency-Key'] = SecureRandom.uuid
    leave_code, rh, leave_response = request(:post, leave_url, headers: leave_headers, body: {})
    record_http(api, "#{game_code} leave account #{account}", :post, leave_url, leave_headers, {}, leave_code, rh, leave_response)
  end
end

File.write(File.join(OUT, '02-hall-api.txt'), api)

delta = +"FW-035 log increments\n"
logs.each do |name, path|
  after = line_count(path)
  delta << "\n===== #{name} =====\npath: #{path}\nbefore_lines: #{before[name]}\nafter_lines: #{after}\nadded_lines: #{after - before[name]}\n"
  delta << appended_lines(path, before[name])
end
File.write(File.join(OUT, '03-success-room-log.txt'), delta)

summary = []
%w[CD201 LS201].each do |code|
  result = results.fetch(code, {})
  summary << "#{code} create_code=#{result[:create_code]} roomId=#{result[:room_id].inspect} join_code=#{result[:join_code].inspect}"
end
mn = results.fetch('MN201', {})
summary << "MN201 create_code=#{mn[:create_code]} response_code=#{mn[:response_code].inspect} response_message=#{mn[:response_message].inspect}"
summary << "7461 curl_code=#{lobby_code}"
File.write(File.join(OUT, '04-summary.md'), summary.join("\n") + "\n")

puts "Evidence written to #{OUT}"
