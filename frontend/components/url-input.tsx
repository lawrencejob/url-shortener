import { Field, FieldLabel, FieldDescription } from "./ui/field";
import { Input } from "./ui/input";

export default function UrlInput() {
    return (
        <Field>
            <FieldLabel htmlFor="newUrl">
                Card Number
            </FieldLabel>
            <Input
                id="newUrl"
                type="url"
                placeholder="https://example.com"
                required
            />
            <FieldDescription>
                Enter the URL you want to shorten
            </FieldDescription>
        </Field>
    )
}