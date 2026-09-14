#!/usr/bin/env ruby
require 'json';require 'rexml/document';require 'fileutils'
root=File.expand_path('..',__dir__);repo=File.expand_path('~/.m2/repository');inventory=JSON.parse(File.read(File.join(root,'docs/generated/pkg02-target-lib-inventory.json')))
components=inventory['jars'].map do|jar|
  coord=jar['repositoryCoordinates'].first;licenses=[];purl=nil
  if coord
    pom=File.join(repo,coord['groupId'].tr('.','/'),coord['artifactId'],coord['version'],"#{coord['artifactId']}-#{coord['version']}.pom")
    if File.file?(pom)
      begin;doc=REXML::Document.new(File.read(pom));REXML::XPath.each(doc,'//*[local-name()="licenses"]/*[local-name()="license"]/*[local-name()="name"]'){|n|licenses<<n.text.to_s.strip};rescue REXML::ParseException;end
    end
    purl="pkg:maven/#{coord['groupId']}/#{coord['artifactId']}@#{coord['version']}"
  end
  {type:'library',name:File.basename(jar['path']),purl:purl,hashes:[{alg:'SHA-256',content:jar['sha256']}],licenses:licenses.uniq.map{|name|{license:{name:name}}},properties:[{name:'aoo:sourcePath',value:jar['path']},{name:'aoo:reactorArtifacts',value:jar['reactorArtifacts'].join(',')}]} 
end
missing=components.select{|c|c[:licenses].empty?}.map{|c|c[:name]};sbom={bomFormat:'CycloneDX',specVersion:'1.6',version:1,components:components};out=File.join(root,'docs/generated/pkg03-target-lib.cdx.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(sbom)+"\n");report={schemaVersion:1,componentCount:components.length,missingLicense:missing,allHashes:true,allSources:components.all?{|c|c[:purl]||!c[:properties][1][:value].empty?},allLicenses:missing.empty?,sbom:'docs/generated/pkg03-target-lib.cdx.json',passed:missing.empty?};File.write(File.join(root,'docs/generated/pkg03-jar-provenance.json'),JSON.pretty_generate(report)+"\n");abort "PKG03 blocked: #{missing.length} jars lack declared license metadata" unless report[:passed]
