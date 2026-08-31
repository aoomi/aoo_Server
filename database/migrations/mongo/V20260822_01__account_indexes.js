const accountInfo = db.getCollection("DbTagAccountInfo");
accountInfo.createIndex({accountState: 1, loginTime: -1}, {name: "stateLoginTime"});

const accountType = db.getCollection("DbTagAccountType");
const duplicateLogin = accountType.aggregate([
    {$group: {_id: {accountType: "$accountType", charAccount: "$charAccount"}, count: {$sum: 1}}},
    {$match: {count: {$gt: 1}}},
    {$limit: 1}
]).hasNext();
const duplicateBinding = accountType.aggregate([
    {$group: {_id: {accountId: "$accountId", accountType: "$accountType"}, count: {$sum: 1}}},
    {$match: {count: {$gt: 1}}},
    {$limit: 1}
]).hasNext();
if (duplicateLogin || duplicateBinding) {
    throw new Error("Duplicate account bindings found; merge them before creating unique indexes");
}

accountType.createIndex(
    {accountType: 1, charAccount: 1},
    {name: "accountTypeCharAccountUnique", unique: true}
);
accountType.createIndex(
    {accountId: 1, accountType: 1},
    {name: "accountIdTypeUnique", unique: true}
);

for (const index of accountInfo.getIndexes()) {
    if (index.name === "charAccount") accountInfo.dropIndex(index.name);
}
for (const index of accountType.getIndexes()) {
    if (index.name === "charAccountIndex" || index.name === "accountIdIndex") {
        accountType.dropIndex(index.name);
    }
}
