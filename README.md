# James LSC plugin

[![Build Status](https://travis-ci.org/lsc-project/lsc-james-plugin.svg?branch=master)](https://travis-ci.org/lsc-project/lsc-james-plugin)

This a plugin for LSC, using James REST API


### Goal

The object of this plugin is to synchronize addresses aliases from one referential to a [James server](https://james.apache.org/).
For example it can be used to synchronize the aliases stored in the LDAP of an OBM instance to the James Server(s) of an OpenPaas deployment.

### Architecture

Given the following LDAP entry:
```
dn: uid=rkowalsky,ou=users,dc=linagora.com,dc=lng
[...]
mail: rkowalsky@linagora.com
mailAlias: remy.kowalsky@linagora.com
mailAlias: remy@linagora.com
```

This will be represented as the following James address alias:
```
$ curl -XGET http://ip:port/address/aliases/rkowalsky@linagora.com

[
  {"source":"remy.kowalsky@linagora.com"},
  {"source":"remy@linagora.com"}
]
```

As addresses aliases in James are only created if there are some sources, an LDAP entry without mailAlias attribute won't be synchronized.

The pivot used for the synchronization in the LSC connector is the email address, here `rkowalsky@linagora.com`.

The destination attribute for the LSC aliases connector is named `sources`.

### Configuration

The plugin connection needs a JWT token to connect to James. To configure this JWT token, set the `password` field of the plugin connection as the JWT token you want to use.

The `url` field of the plugin connection must be set to the URL of James' webadmin.

The `username` field of the plugin is ignored for now.

### Usage

WIP

### Packaging

WIP
