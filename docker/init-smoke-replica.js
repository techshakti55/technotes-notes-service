// Idempotent initialization for this isolated local test database.
try {
    rs.status();
} catch (error) {
    if (error.code !== 94 && error.codeName !== "NotYetInitialized") {
        throw error;
    }
    const result = rs.initiate({
        _id: "rs0",
        members: [{_id: 0, host: "mongodb:27017"}]
    });
    if (result.ok !== 1) {
        throw new Error("Replica set initialization failed: " + JSON.stringify(result));
    }
}

const deadline = Date.now() + 60000;
while (Date.now() < deadline) {
    if (db.hello().isWritablePrimary === true) {
        print("Smoke replica set primary is ready.");
        quit(0);
    }
    sleep(500);
}
throw new Error("Replica set primary did not become ready within 60 seconds.");
